package com.junaidjamshid.i211203.presentation.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityEditProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Instagram-style Edit Profile Activity — pixel-perfect UI.
 */
@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    private var profileImageBytes: ByteArray? = null
    private var isFirstLoad = true

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    binding.profileImage.setImageBitmap(bitmap)

                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    profileImageBytes = stream.toByteArray()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                binding.profileImage.setImageBitmap(it)

                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                profileImageBytes = stream.toByteArray()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeUiState()

        viewModel.loadCurrentUserProfile()
    }

    private fun setupClickListeners() {
        // Cancel button → close activity
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Done button → save changes
        binding.btnDone.setOnClickListener {
            saveProfile()
        }

        // Profile image container tap
        binding.profileImageContainer.setOnClickListener {
            showImagePickerDialog()
        }

        // "Edit picture or avatar" text tap
        binding.btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        // Professional account link
        binding.btnProfessionalAccount.setOnClickListener {
            Toast.makeText(this, "Switch to professional account", Toast.LENGTH_SHORT).show()
        }

        // Personal info link
        binding.btnPersonalInfo.setOnClickListener {
            Toast.makeText(this, "Personal information settings", Toast.LENGTH_SHORT).show()
        }

        // Add links
        binding.btnAddLinks.setOnClickListener {
            Toast.makeText(this, "Add links", Toast.LENGTH_SHORT).show()
        }

        // Gender selector
        binding.tvGender.setOnClickListener {
            showGenderPicker()
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: ProfileUiState) {
        // Only populate fields on first load to avoid overwriting user edits
        if (isFirstLoad) {
            state.user?.let { user ->
                binding.etName.setText(user.fullName)
                binding.etUsername.setText(user.username)
                binding.etPhone.setText(user.phoneNumber)
                binding.etBio.setText(user.bio)

                // Load profile image
                if (!user.profilePicture.isNullOrEmpty() && profileImageBytes == null) {
                    try {
                        val imageBytes = android.util.Base64.decode(
                            user.profilePicture,
                            android.util.Base64.DEFAULT
                        )
                        profileImageBytes = imageBytes
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        binding.profileImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        binding.profileImage.setImageResource(R.drawable.default_profile)
                    }
                } else if (profileImageBytes == null) {
                    binding.profileImage.setImageResource(R.drawable.default_profile)
                }

                isFirstLoad = false
            }
        }

        // Handle profile update success
        if (state.profileUpdateSuccess) {
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            viewModel.clearProfileUpdateSuccess()
            finish()
        }

        // Handle loading state
        binding.btnDone.isEnabled = !state.isLoading
        binding.btnDone.alpha = if (state.isLoading) 0.5f else 1.0f

        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Library", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Change Profile Photo")
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
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun showGenderPicker() {
        val genders = arrayOf("Male", "Female", "Custom", "Prefer not to say")

        AlertDialog.Builder(this)
            .setTitle("Gender")
            .setItems(genders) { _, which ->
                binding.tvGender.text = genders[which]
            }
            .show()
    }

    private fun saveProfile() {
        val fullName = binding.etName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        if (fullName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Name and username are required", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.updateProfile(fullName, username, phone, bio, profileImageBytes)
    }
}
