package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.models.User
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.io.IOException

class EditProfile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var profileImageView: CircleImageView
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var doneButton: TextView
    private lateinit var progressDialog: ProgressDialog

    private var profileImageByteArray: ByteArray? = null
    private var currentUser: User? = null

    // Image selection launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                try {
                    // Load and set the selected image
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    profileImageView.setImageBitmap(bitmap)

                    // Convert bitmap to ByteArray
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    profileImageByteArray = stream.toByteArray()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Camera launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val imageBitmap = data?.extras?.get("data") as? Bitmap

            imageBitmap?.let { bitmap ->
                // Set the captured image
                profileImageView.setImageBitmap(bitmap)

                // Convert bitmap to ByteArray
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                profileImageByteArray = stream.toByteArray()
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize views
        profileImageView = findViewById(R.id.profile_image)
        nameEditText = findViewById(R.id.et_name)
        usernameEditText = findViewById(R.id.et_username)
        phoneEditText = findViewById(R.id.et_phone)
        bioEditText = findViewById(R.id.et_bio)
        doneButton = findViewById(R.id.btn_done)

        // Initialize progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Updating profile...")
        progressDialog.setCancelable(false)

        // Set up click listener for profile image
        val imageContainer = findViewById<FrameLayout>(R.id.profile_image_container)
        imageContainer.setOnClickListener {
            showImagePickerDialog()
        }

        // Set up click listener for done button
        doneButton.setOnClickListener {
            saveUserProfile()
        }

        // Load current user data
        loadUserData()
    }

    private fun loadUserData() {
        progressDialog.setMessage("Loading profile...")
        progressDialog.show()

        val userId = auth.currentUser?.uid

        if (userId != null) {
            database.child("Users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    progressDialog.dismiss()

                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        currentUser = user

                        user?.let {
                            // Set text fields
                            nameEditText.setText(it.fullName)
                            usernameEditText.setText(it.username)
                            phoneEditText.setText(it.phoneNumber)
                            bioEditText.setText(it.bio)

                            // Load profile image if exists
                            val profilePictureBytes = it.getProfilePictureBytes()
                            profilePictureBytes?.let { bytes ->
                                profileImageByteArray = bytes
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                profileImageView.setImageBitmap(bitmap)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                    Toast.makeText(this@EditProfile, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            progressDialog.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun saveUserProfile() {
        val name = nameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val bio = bioEditText.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name, username and phone are required", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Create user updates map
            val userUpdates = HashMap<String, Any?>()
            userUpdates["fullName"] = name
            userUpdates["username"] = username
            userUpdates["phoneNumber"] = phone
            userUpdates["bio"] = bio

            // Update profile picture if changed
            if (profileImageByteArray != null) {
                val base64Image = Base64.encodeToString(profileImageByteArray, Base64.DEFAULT)
                userUpdates["profilePicture"] = base64Image
            }

            database.child("Users").child(userId).updateChildren(userUpdates)
                .addOnCompleteListener { task ->
                    progressDialog.dismiss()

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            progressDialog.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}