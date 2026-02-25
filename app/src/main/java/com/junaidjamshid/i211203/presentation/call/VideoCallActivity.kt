package com.junaidjamshid.i211203.presentation.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.databinding.ActivityVideoCallsBinding
import dagger.hilt.android.AndroidEntryPoint
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.launch

/**
 * Clean Architecture Video Call Activity.
 */
@AndroidEntryPoint
class VideoCallActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVideoCallsBinding
    private val viewModel: CallViewModel by viewModels()
    
    // Agora SDK
    private val appId = "d552198c4ec34f59846c51ec0dba73c4"
    private var rtcEngine: RtcEngine? = null
    private var channelName: String = ""
    private var isCaller: Boolean = false
    private var receiverUserId: String = ""
    
    private val PERMISSION_REQ_ID = 22
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    
    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Connected to call", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                setupRemoteVideo(uid)
                Toast.makeText(this@VideoCallActivity, "Remote user joined", Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Remote user left", Toast.LENGTH_SHORT).show()
                endCall()
            }
        }
        
        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Error: $err", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityVideoCallsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "test_channel"
        receiverUserId = intent.getStringExtra("USER_ID") ?: ""
        isCaller = intent.getBooleanExtra("IS_CALLER", false)
        
        setupClickListeners()
        observeUiState()
        
        if (checkPermissions()) {
            initializeAndJoinChannel()
        } else {
            requestPermissions()
        }
    }
    
    private fun setupClickListeners() {
        binding.EndCall.setOnClickListener {
            endCall()
        }
        
        binding.btnToggleVideo.setOnClickListener {
            viewModel.toggleVideo()
            updateVideoState()
        }
        
        binding.btnToggleMic.setOnClickListener {
            viewModel.toggleMic()
            updateMicState()
        }
        
        binding.btnToggleSpeaker.setOnClickListener {
            viewModel.toggleSpeaker()
            updateSpeakerState()
        }
        
        binding.btnFlipCamera.setOnClickListener {
            rtcEngine?.switchCamera()
        }
    }
    
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update call duration
                    binding.txtCallDuration.text = formatDuration(state.callDuration)
                    
                    // Handle error
                    state.error?.let { error ->
                        Toast.makeText(this@VideoCallActivity, error, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
    
    private fun checkPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQ_ID)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeAndJoinChannel()
            } else {
                Toast.makeText(this, "Permissions required for video call", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun initializeAndJoinChannel() {
        try {
            val config = RtcEngineConfig().apply {
                mContext = applicationContext
                mAppId = appId
                mEventHandler = rtcEventHandler
            }
            
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.enableVideo()
            
            setupLocalVideo()
            
            rtcEngine?.joinChannel(null, channelName, "", 0)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupLocalVideo() {
        val localVideoContainer = binding.localVideoContainer
        val localSurfaceView = SurfaceView(this)
        localSurfaceView.setZOrderMediaOverlay(true)
        localVideoContainer.addView(localSurfaceView)
        
        rtcEngine?.setupLocalVideo(
            VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        )
        rtcEngine?.startPreview()
    }
    
    private fun setupRemoteVideo(uid: Int) {
        val remoteVideoContainer = binding.remoteVideoContainer
        remoteVideoContainer.removeAllViews()
        
        val remoteSurfaceView = SurfaceView(this)
        remoteVideoContainer.addView(remoteSurfaceView)
        
        rtcEngine?.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }
    
    private fun updateVideoState() {
        val isEnabled = viewModel.uiState.value.isVideoEnabled
        rtcEngine?.muteLocalVideoStream(!isEnabled)
    }
    
    private fun updateMicState() {
        val isEnabled = viewModel.uiState.value.isMicEnabled
        rtcEngine?.muteLocalAudioStream(!isEnabled)
    }
    
    private fun updateSpeakerState() {
        val isEnabled = viewModel.uiState.value.isSpeakerEnabled
        rtcEngine?.setEnableSpeakerphone(isEnabled)
    }
    
    private fun endCall() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        viewModel.endCall()
        finish()
    }
    
    private fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
