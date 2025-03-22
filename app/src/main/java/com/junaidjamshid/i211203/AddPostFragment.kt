package com.junaidjamshid.i211203

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.models.Comment
import com.junaidjamshid.i211203.models.Post
import java.io.ByteArrayOutputStream
import java.util.*

class AddPostFragment : Fragment() {

    // UI components
    private lateinit var backButton: ImageView
    private lateinit var imagePlaceholder: FrameLayout
    private lateinit var postImage: ImageView
    private lateinit var selectImageButton: ImageButton
    private lateinit var captionInput: EditText
    private lateinit var shareButton: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Image URI
    private var selectedImageUri: Uri? = null

    // Request code for image selection
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_post, container, false)

        // Initialize UI components
        backButton = view.findViewById(R.id.back_button)
        imagePlaceholder = view.findViewById(R.id.image_placeholder)
        postImage = view.findViewById(R.id.post_image)
        selectImageButton = view.findViewById(R.id.select_image_button)
        captionInput = view.findViewById(R.id.caption_input)
        shareButton = view.findViewById(R.id.share)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Set click listeners
        setupClickListeners()

        return view
    }

    private fun setupClickListeners() {
        // Back button click listener
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Select image button click listener
        selectImageButton.setOnClickListener {
            openGallery()
        }

        // Also make the placeholder clickable to select image
        imagePlaceholder.setOnClickListener {
            openGallery()
        }

        // Share button click listener
        shareButton.setOnClickListener {
            if (validatePost()) {
                uploadPostToFirebase()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data

            // Display the selected image
            postImage.visibility = View.VISIBLE
            postImage.setImageURI(selectedImageUri)

            // Hide the select image button
            selectImageButton.visibility = View.GONE
        }
    }

    private fun validatePost(): Boolean {
        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            return false
        }

        // Caption is optional, so we don't check for it
        return true
    }

    private fun uploadPostToFirebase() {
        // Show loading
        showLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        // Get current user ID
        val userId = currentUser.uid

        // Get the caption text
        val caption = captionInput.text.toString().trim()

        // Convert image to Base64 string
        val imageBase64 = convertImageToBase64()
        if (imageBase64 == null) {
            Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        // Get user information to add to the post
        getUserInfo(userId) { username, profileImageUrl ->
            // Create post ID
            val postId = database.reference.child("posts").push().key ?: return@getUserInfo

            // Create post object
            val post = Post(
                postId = postId,
                userId = userId,
                username = username,
                userProfileImage = profileImageUrl,
                postImageUrl = imageBase64,
                caption = caption,
                timestamp = System.currentTimeMillis(),
                likes = mutableMapOf(),
                comments = mutableListOf()
            )

            // Save post to Firebase Realtime Database
            database.reference.child("posts").child(postId).setValue(post)
                .addOnSuccessListener {
                    Toast.makeText(context, "Post uploaded successfully", Toast.LENGTH_SHORT).show()



                    // Clear inputs and go back
                    clearInputs()
                    showLoading(false)

                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to upload post: ${e.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
        }
    }

    private fun convertImageToBase64(): String? {
        try {
            val inputStream = context?.contentResolver?.openInputStream(selectedImageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize bitmap to reduce file size
            val resizedBitmap = resizeBitmap(bitmap, 800)

            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        val ratio = width.toFloat() / height.toFloat()

        if (ratio > 1) {
            // Landscape image
            width = maxSize
            height = (width / ratio).toInt()
        } else {
            // Portrait image
            height = maxSize
            width = (height * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun getUserInfo(userId: String, callback: (String, String) -> Unit) {
        database.reference.child("Users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                val profileImageUrl = snapshot.child("profilePicture").getValue(String::class.java) ?: ""

                callback(username, profileImageUrl)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to get user info: ${error.message}", Toast.LENGTH_SHORT).show()
                callback("Unknown", "")
            }
        })
    }

    private fun clearInputs() {
        selectedImageUri = null
        postImage.visibility = View.GONE
        selectImageButton.visibility = View.VISIBLE
        captionInput.text.clear()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            shareButton.isEnabled = false
            shareButton.text = "Uploading..."
        } else {
            shareButton.isEnabled = true
            shareButton.text = "Share"
        }
    }
}