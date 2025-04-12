package com.junaidjamshid.i211203

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class VideoCalls : AppCompatActivity() {
    // Agora SDK
    private val appId = "d552198c4ec34f59846c51ec0dba73c4" // Your Agora App ID

    // Don't hardcode the token - get it from your server in production
    // Use a temporary token for testing only
    private var token: String? = null
    private var channelName: String = ""
    private var mRtcEngine: RtcEngine? = null
    private var isCalling = false

    // UI Components
    private lateinit var endCallButton: ImageView
    private lateinit var toggleVideoButton: ImageView
    private lateinit var toggleMicButton: ImageView
    private lateinit var toggleSpeakerButton: ImageView
    private lateinit var flipCameraButton: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var callDurationTextView: TextView

    // Call state
    private var isVideoEnabled = true
    private var isMicEnabled = true
    private var isSpeakerEnabled = true
    private var isCaller = false
    private var receiverUserId = ""

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Permission
    private val PERMISSION_REQ_ID = 22

    // Agora event handler
    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("AgoraEvent", "Successfully joined channel: $channel")
                showToast("Connected to call")
                isCalling = true
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.d("AgoraEvent", "Remote user joined: $uid")
                setupRemoteVideo(uid)
                showToast("Remote user joined")
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Log.d("AgoraEvent", "Remote user left: $uid, reason: $reason")
                showToast("Remote user left")
                onRemoteUserLeft()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Log.e("AgoraEvent", "Error occurred: $err")
                showToast("Error: $err")
                if (err == Constants.ERR_JOIN_CHANNEL_REJECTED) {
                    showToast("Join channel rejected. Please check your token.")
                    finish()
                } else if (err == Constants.ERR_INVALID_TOKEN) {
                    showToast("Invalid token. Please regenerate a new token.")
                } else if (err == Constants.ERR_TOKEN_EXPIRED) {
                    showToast("Token expired. Please regenerate a new token.")
                }
            }
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            runOnUiThread {
                Log.d("AgoraEvent", "Token will expire soon")
                showToast("Call connection is expiring soon")
                // Here you would request a new token from your server
                // and then call renewToken method
            }
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            runOnUiThread {
                when (state) {
                    Constants.CONNECTION_STATE_CONNECTING ->
                        Log.d("AgoraEvent", "Connecting...")
                    Constants.CONNECTION_STATE_CONNECTED ->
                        Log.d("AgoraEvent", "Connected")
                    Constants.CONNECTION_STATE_FAILED -> {
                        Log.e("AgoraEvent", "Connection failed. Reason: $reason")
                        showToast("Connection failed. Error code: $reason")
                        if (reason == Constants.CONNECTION_CHANGED_INVALID_TOKEN) {
                            showToast("Invalid token. Please regenerate a new token.")
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_video_calls)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get intent data
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "i211203"
        receiverUserId = intent.getStringExtra("USER_ID") ?: ""
        isCaller = intent.getBooleanExtra("IS_CALLER", false)

        // For testing, use token from intent or generate/fetch one
        token = intent.getStringExtra("TOKEN") ?: fetchToken()

        if (channelName.isEmpty() || receiverUserId.isEmpty()) {
            showToast("Invalid call parameters")
            finish()
            return
        }

        Log.d("VideoCalls", "Channel: $channelName, Receiver: $receiverUserId, Caller: $isCaller, Token: $token")

        // Initialize UI components
        initializeUI()

        // Load receiver's username
        loadUserDetails(receiverUserId)

        // Check and request permissions
        if (checkPermissions()) {
            initializeAndStartCall()
        } else {
            requestPermissions()
        }
    }

    // This would be replaced with an actual API call to your token server
    private fun fetchToken(): String? {
        // In a real app, you would fetch this from your server
        // For testing purposes, use a temporary token from Agora Console
        return null // For testing without token (only works with certain app configurations)

        // Or use a temporary token (replace with your own)
        // return "your_temporary_token_here"
    }

    private fun initializeUI() {
        endCallButton = findViewById(R.id.EndCall)
        toggleVideoButton = findViewById(R.id.btnToggleVideo)
        toggleMicButton = findViewById(R.id.btnToggleMic)
        toggleSpeakerButton = findViewById(R.id.btnToggleSpeaker)
        flipCameraButton = findViewById(R.id.btnFlipCamera)
        userNameTextView = findViewById(R.id.txtCallerName)
        callDurationTextView = findViewById(R.id.txtCallDuration)

        // Set button click listeners
        endCallButton.setOnClickListener {
            endCall()
        }

        toggleVideoButton.setOnClickListener {
            toggleVideo()
        }

        toggleMicButton.setOnClickListener {
            toggleMic()
        }

        toggleSpeakerButton.setOnClickListener {
            toggleSpeaker()
        }

        flipCameraButton.setOnClickListener {
            flipCamera()
        }
    }

    private fun loadUserDetails(userId: String) {
        val userRef = database.reference.child("Users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.child("username").getValue(String::class.java) ?: "Unknown User"
                    userNameTextView.text = userName
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load user details")
            }
        })
    }

    private fun initializeAndStartCall() {
        try {
            // Initialize Agora SDK
            initializeAgoraEngine()

            // Enable video
            enableVideo()

            // Setup local video view
            setupLocalVideo()

            // Join channel
            joinChannel()

            // Add call to Firebase (for call history or notification)
            if (isCaller) {
                addCallToFirebase()
            }
        } catch (e: Exception) {
            Log.e("InitCall", "Error initializing call: ${e.message}", e)
            showToast("Failed to initialize call: ${e.message}")
            finish()
        }
    }

    private fun initializeAgoraEngine() {
        try {
            Log.d("AgoraInit", "Initializing Agora engine with appId: $appId")

            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = appId
                mEventHandler = mRtcEventHandler
            }

            mRtcEngine = RtcEngine.create(config)
            Log.d("AgoraInit", "Agora engine initialized successfully")
        } catch (e: Exception) {
            val errorMessage = "Error initializing Agora engine: ${e.message}"
            Log.e("AgoraError", errorMessage, e)
            e.printStackTrace() // More detailed error information
            showToast(errorMessage)
            throw e
        }
    }

    private fun enableVideo() {
        try {
            mRtcEngine?.apply {
                enableVideo()
                startPreview()
                setDefaultAudioRoutetoSpeakerphone(true)
                Log.d("AgoraVideo", "Video enabled and preview started")
            }
        } catch (e: Exception) {
            Log.e("AgoraVideo", "Error enabling video: ${e.message}", e)
            showToast("Error enabling video: ${e.message}")
        }
    }

    private fun setupLocalVideo() {
        try {
            val localContainer = findViewById<FrameLayout>(R.id.localVideoContainer)
            localContainer.removeAllViews()

            val surfaceView = SurfaceView(baseContext)
            surfaceView.setZOrderMediaOverlay(true)
            localContainer.addView(surfaceView)

            mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            Log.d("AgoraVideo", "Local video setup complete")
        } catch (e: Exception) {
            Log.e("AgoraVideo", "Error setting up local video: ${e.message}", e)
            showToast("Error setting up local video: ${e.message}")
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        try {
            val remoteContainer = findViewById<FrameLayout>(R.id.remoteVideoContainer)
            remoteContainer.removeAllViews()

            val surfaceView = SurfaceView(baseContext)
            remoteContainer.addView(surfaceView)

            mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
            Log.d("AgoraVideo", "Remote video setup complete for uid: $uid")
        } catch (e: Exception) {
            Log.e("AgoraVideo", "Error setting up remote video: ${e.message}", e)
            showToast("Error setting up remote video: ${e.message}")
        }
    }

    private fun joinChannel() {
        try {
            val options = io.agora.rtc2.ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishMicrophoneTrack = true
                publishCameraTrack = true
            }

            Log.d("AgoraJoin", "Attempting to join channel: $channelName with token: $token")
            val result = mRtcEngine?.joinChannel(token, channelName, 0, options)
            Log.d("AgoraJoin", "Join channel result: $result")

            if (result != 0) {
                showToast("Failed to join channel with error code: $result")
            }
        } catch (e: Exception) {
            Log.e("AgoraJoin", "Error joining channel: ${e.message}", e)
            showToast("Failed to join channel: ${e.message}")
        }
    }

    private fun addCallToFirebase() {
        val callsRef = database.reference.child("Calls").push()
        val callId = callsRef.key ?: return

        val callData = HashMap<String, Any>().apply {
            put("callerId", auth.currentUser?.uid ?: "")
            put("receiverId", receiverUserId)
            put("channelName", channelName)
            put("timestamp", System.currentTimeMillis())
            put("type", "video")
            put("status", "ongoing")
        }

        callsRef.setValue(callData)
            .addOnSuccessListener {
                Log.d("Firebase", "Call record added successfully")
            }
            .addOnFailureListener {
                Log.e("Firebase", "Failed to record call", it)
                showToast("Failed to record call")
            }
    }

    private fun updateCallStatus(status: String) {
        val callsRef = database.reference.child("Calls")
        callsRef.orderByChild("channelName").equalTo(channelName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (callSnapshot in snapshot.children) {
                        callSnapshot.ref.child("status").setValue(status)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Failed to update call status", error.toException())
                }
            })
    }

    private fun endCall() {
        if (isCalling) {
            updateCallStatus("ended")
        }

        try {
            mRtcEngine?.apply {
                stopPreview()
                leaveChannel()
            }
            Log.d("AgoraEnd", "Successfully left channel")
        } catch (e: Exception) {
            Log.e("AgoraEnd", "Error ending call: ${e.message}", e)
        }

        finish()
    }

    private fun onRemoteUserLeft() {
        val remoteContainer = findViewById<FrameLayout>(R.id.remoteVideoContainer)
        remoteContainer.removeAllViews()

        // Show toast and end call after a delay
        showToast("Call ended")
        remoteContainer.postDelayed({
            endCall()
        }, 2000)
    }

    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        mRtcEngine?.enableLocalVideo(isVideoEnabled)

        toggleVideoButton.setImageResource(
            if (isVideoEnabled) R.drawable.video_camera else R.drawable.video_camera
        )
        Log.d("AgoraControls", "Video enabled: $isVideoEnabled")
    }

    private fun toggleMic() {
        isMicEnabled = !isMicEnabled
        mRtcEngine?.enableLocalAudio(isMicEnabled)

        toggleMicButton.setImageResource(
            if (isMicEnabled) R.drawable.mute else R.drawable.mute
        )
        Log.d("AgoraControls", "Mic enabled: $isMicEnabled")
    }

    private fun toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled
        mRtcEngine?.setEnableSpeakerphone(isSpeakerEnabled)

        toggleSpeakerButton.setImageResource(
            if (isSpeakerEnabled) R.drawable.loud_speaker else R.drawable.loud_speaker
        )
        Log.d("AgoraControls", "Speaker enabled: $isSpeakerEnabled")
    }

    private fun flipCamera() {
        try {
            mRtcEngine?.switchCamera()
            Log.d("AgoraControls", "Camera flipped")
        } catch (e: Exception) {
            Log.e("AgoraControls", "Error flipping camera: ${e.message}", e)
            showToast("Failed to switch camera")
        }
    }

    private fun checkPermissions(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQ_ID)
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.INTERNET
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("Permissions", "All permissions granted")
                initializeAndStartCall()
            } else {
                Log.e("Permissions", "Permissions denied")
                showToast("Permissions denied. Cannot start call.")
                finish()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent accidental back press during a call
        showToast("Use the end call button to exit")
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isCalling) {
            updateCallStatus("ended")
        }

        try {
            mRtcEngine?.apply {
                stopPreview()
                leaveChannel()
            }
            RtcEngine.destroy()
            Log.d("AgoraDestroy", "Agora engine destroyed")
        } catch (e: Exception) {
            Log.e("AgoraDestroy", "Error destroying Agora engine: ${e.message}", e)
        }
    }
}