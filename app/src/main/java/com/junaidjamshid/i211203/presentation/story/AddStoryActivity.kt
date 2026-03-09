package com.junaidjamshid.i211203.presentation.story

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.junaidjamshid.i211203.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Instagram-style Add Story Activity.
 * User picks an image from gallery and shares it as a 24h story.
 */
@AndroidEntryPoint
class AddStoryActivity : AppCompatActivity() {

    private val viewModel: StoryViewModel by viewModels()

    private lateinit var imgPreview: ImageView
    private lateinit var placeholderContainer: LinearLayout
    private lateinit var btnClose: ImageView
    private lateinit var btnGallery: MaterialButton
    private lateinit var btnShareStory: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var selectedImageBytes: ByteArray? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_add_story)

        imgPreview = findViewById(R.id.img_story_preview)
        placeholderContainer = findViewById(R.id.placeholder_container)
        btnClose = findViewById(R.id.btn_close)
        btnGallery = findViewById(R.id.btn_gallery)
        btnShareStory = findViewById(R.id.btn_share_story)
        progressBar = findViewById(R.id.progress_bar)

        setupClickListeners()
        observeState()

        // Auto-open gallery on launch
        galleryLauncher.launch("image/*")
    }

    private fun setupClickListeners() {
        btnClose.setOnClickListener { finish() }

        btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnShareStory.setOnClickListener {
            selectedImageBytes?.let { bytes ->
                viewModel.createStory(bytes)
            }
        }

        placeholderContainer.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun handleImageSelected(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bytes = inputStream.readBytes()
            inputStream.close()

            // Compress to reasonable size
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            selectedImageBytes = outputStream.toByteArray()

            // Show preview
            imgPreview.setImageBitmap(bitmap)
            imgPreview.visibility = View.VISIBLE
            placeholderContainer.visibility = View.GONE
            btnGallery.visibility = View.GONE
            btnShareStory.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Loading
                    if (state.isLoading) {
                        progressBar.visibility = View.VISIBLE
                        btnShareStory.visibility = View.GONE
                    } else {
                        progressBar.visibility = View.GONE
                    }

                    // Story created successfully
                    if (state.storyCreated) {
                        viewModel.resetStoryCreated()
                        Toast.makeText(this@AddStoryActivity, "Story shared!", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    // Error
                    state.error?.let { error ->
                        Toast.makeText(this@AddStoryActivity, error, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                        btnShareStory.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}
