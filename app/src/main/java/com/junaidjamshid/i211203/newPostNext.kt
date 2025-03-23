package com.junaidjamshid.i211203

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.models.Story
import com.junaidjamshid.i211203.models.User
import java.io.ByteArrayOutputStream
import java.util.*

class newPostNext : AppCompatActivity() {
    private lateinit var closeButton: ImageView
    private lateinit var shareStoryButton: TextView
    private lateinit var storyImagePreview: ImageView
    private lateinit var captionInput: EditText
    private lateinit var uploadPhotoText: TextView
    private lateinit var captureButton: ImageView
    private lateinit var flipCameraButton: ImageView

    private val TAG = "NewPostNext"
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var selectedImageUri: Uri? = null
    private var imageByteArray: ByteArray? = null
    private var currentUser: User? = null

    // Register for gallery result
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                storyImagePreview.setImageBitmap(bitmap)
                imageByteArray = convertBitmapToByteArray(bitmap)
                uploadPhotoText.text = "Change Photo"
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image: ${e.message}")
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Register for camera result
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            selectedImageUri?.let {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    storyImagePreview.setImageBitmap(bitmap)
                    imageByteArray = convertBitmapToByteArray(bitmap)
                    uploadPhotoText.text = "Change Photo"
                } catch (e: Exception) {
                    Log.e(TAG, "Error capturing image: ${e.message}")
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_new_post_next)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI elements
        initializeViews()

        // Set click listeners
        setupClickListeners()

        // Load current user data
        loadCurrentUserData()
    }

    private fun initializeViews() {
        closeButton = findViewById(R.id.close_button)
        shareStoryButton = findViewById(R.id.share_story_button)
        storyImagePreview = findViewById(R.id.story_image_preview)
        captionInput = findViewById(R.id.caption_input)
        uploadPhotoText = findViewById(R.id.story_action_text)
        captureButton = findViewById(R.id.capture_button)
        flipCameraButton = findViewById(R.id.flip_camera)
    }

    private fun setupClickListeners() {
        closeButton.setOnClickListener {
            finish()
        }

        shareStoryButton.setOnClickListener {
            uploadStory()
        }

        captureButton.setOnClickListener {
            if (checkCameraPermission()) {
                openGallery()
            } else {
                requestCameraPermission()
            }
        }

        flipCameraButton.setOnClickListener {
            Toast.makeText(this, "Camera flip not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentUserData() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userRef = database.getReference("Users").child(currentUserId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    currentUser = snapshot.getValue(User::class.java)
                    if (currentUser == null) {
                        Toast.makeText(this@newPostNext, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@newPostNext, "User data not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading user data: ${error.message}")
                Toast.makeText(this@newPostNext, "Failed to load user data", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun uploadStory() {
        if (imageByteArray == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser == null) {
            Toast.makeText(this, "User data not loaded, please try again", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator if you have one

        // Generate a new story ID
        val storyId = database.getReference("stories").push().key
        if (storyId == null) {
            Toast.makeText(this, "Failed to create story", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current timestamp
        val currentTime = System.currentTimeMillis()
        // Set expiry to 24 hours from now
        val expiryTime = currentTime + (24 * 60 * 60 * 1000)

        // Convert image to Base64 string for storage
        val imageBase64 = Base64.encodeToString(imageByteArray, Base64.DEFAULT)

        // Create a new story object
        val story = Story(
            storyId = storyId,
            userId = currentUser!!.userId,
            username = currentUser!!.username,
            userProfileImage = currentUser!!.profilePicture ?: "",
            storyImageUrl = imageBase64,
            caption = captionInput.text.toString().trim(),
            timestamp = currentTime,
            expiryTimestamp = expiryTime,
            viewers = mutableListOf()
        )

        // Update the user's storyExpiryTimestamp
        val userRef = database.getReference("Users").child(currentUser!!.userId)
        userRef.child("storyExpiryTimestamp").setValue(expiryTime)

        // Save the story to Firebase
        val storyRef = database.getReference("stories").child(storyId)
        storyRef.setValue(story)
            .addOnSuccessListener {
                Toast.makeText(this, "Story uploaded successfully", Toast.LENGTH_SHORT).show()

                // Schedule a job to delete the story after 24 hours
                scheduleStoryDeletion(storyId, expiryTime)

                // Navigate back to the main activity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading story: ${e.message}")
                Toast.makeText(this, "Failed to upload story: ${e.message}", Toast.LENGTH_SHORT).show()
                // Hide loading indicator if you have one
            }
    }

    private fun scheduleStoryDeletion(storyId: String, expiryTime: Long) {
        // For automatic deletion, Firebase has a TTL (Time to Live) feature in Realtime Database
        // But since we want to update the user's storyExpiryTimestamp as well, we'll set up
        // a cloud function or server-side logic to handle this.

        // This is a placeholder for the actual implementation
        // In a production app, you would use Firebase Cloud Functions or a server-side solution
        Log.d(TAG, "Story $storyId scheduled for deletion at $expiryTime")

        // For now, we'll rely on client-side checking of expiry timestamps when stories are loaded
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            REQUEST_CAMERA_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                Toast.makeText(this, "Permission required to access gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress the image (adjust quality as needed)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
    }
}