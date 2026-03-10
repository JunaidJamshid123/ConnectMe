package com.junaidjamshid.i211203.presentation.post

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.FragmentPostBinding
import com.junaidjamshid.i211203.presentation.main.MainActivityNew
import com.junaidjamshid.i211203.presentation.post.adapter.ImageCarouselAdapter
import com.junaidjamshid.i211203.presentation.post.adapter.SelectedImagesAdapter
import com.junaidjamshid.i211203.util.VideoCompressorUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * Instagram-style Add Post Fragment with multi-image carousel,
 * location, music, and more. Now supports video posts.
 */
@AndroidEntryPoint
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class AddPostFragmentNew : Fragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels()

    // Adapters
    private lateinit var carouselAdapter: ImageCarouselAdapter
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter

    // Track bitmaps for preview
    private val selectedBitmaps = mutableListOf<Bitmap>()
    
    // Video support
    private var selectedVideoUri: Uri? = null
    private var videoPlayer: ExoPlayer? = null
    private var isVideoMode = false
    private var videoThumbnail: Bitmap? = null

    // Multi-image picker
    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()

            // Handle multiple selection
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            }

            // Handle single selection
            if (uris.isEmpty()) {
                result.data?.data?.let { uri -> uris.add(uri) }
            }

            if (uris.isNotEmpty()) {
                processSelectedImages(uris)
            }
        }
    }
    
    // Video picker launcher
    private val pickVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                processSelectedVideo(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupClickListeners()
        setupCaptionWatcher()
        observeUiState()
    }

    private fun setupAdapters() {
        // Carousel adapter for ViewPager2 image preview
        carouselAdapter = ImageCarouselAdapter()
        binding.imagePager.adapter = carouselAdapter
        binding.imagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.setPreviewIndex(position)
                selectedImagesAdapter.setSelectedPosition(position)
                updateDots(position)
            }
        })

        // Selected images thumbnail strip
        selectedImagesAdapter = SelectedImagesAdapter(
            onRemoveClick = { index -> removeImage(index) },
            onItemClick = { index -> binding.imagePager.currentItem = index }
        )
        binding.rvSelectedImages.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSelectedImages.adapter = selectedImagesAdapter
    }

    private fun setupClickListeners() {
        // Back / close button
        binding.backButton.setOnClickListener {
            (activity as? MainActivityNew)?.navigateToTab(R.id.nav_home)
        }

        // Select image area
        binding.selectImageButton.setOnClickListener { openGallery() }
        binding.imagePlaceholder.setOnClickListener {
            if (selectedBitmaps.isEmpty()) openGallery()
        }

        // Share button
        binding.share.setOnClickListener { sharePost() }

        // Tag people row
        binding.rowTagPeople.setOnClickListener {
            Toast.makeText(requireContext(), "Tag people coming soon", Toast.LENGTH_SHORT).show()
        }

        // Add location row
        binding.rowAddLocation.setOnClickListener { showLocationDialog() }

        // Add music row
        binding.rowAddMusic.setOnClickListener { showMusicPicker() }

        // Accessibility / alt text
        binding.rowAltText.setOnClickListener {
            Toast.makeText(requireContext(), "Alt text coming soon", Toast.LENGTH_SHORT).show()
        }

        // Advanced settings
        binding.rowAdvanced.setOnClickListener {
            Toast.makeText(requireContext(), "Advanced settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCaptionWatcher() {
        binding.captionInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setCaption(s.toString().trim())
            }
        })
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addPostUiState.collect { state ->
                    handleUiState(state)
                }
            }
        }
    }

    private fun handleUiState(state: AddPostUiState) {
        // Loading
        binding.loadingOverlay.isVisible = state.isLoading
        binding.share.isEnabled = !state.isLoading
        binding.share.alpha = if (state.isLoading) 0.5f else 1.0f

        // Location text
        if (state.location.isNotEmpty()) {
            binding.tvLocation.text = state.location
            binding.tvLocation.setTextColor(0xFF262626.toInt())
        } else {
            binding.tvLocation.text = "Add location"
            binding.tvLocation.setTextColor(0xFF262626.toInt())
        }

        // Music text
        if (state.musicName.isNotEmpty()) {
            binding.tvMusic.text = "${state.musicName} · ${state.musicArtist}"
            binding.tvMusic.setTextColor(0xFF0095F6.toInt())
        } else {
            binding.tvMusic.text = "Add music"
            binding.tvMusic.setTextColor(0xFF262626.toInt())
        }

        // Post created
        if (state.postCreated) {
            Toast.makeText(requireContext(), "Post shared successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetPostCreated()
            resetForm()
            (activity as? MainActivityNew)?.navigateToHomeAndRefresh()
        }

        // Error
        state.error?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // ====== Media Selection ======

    private fun openGallery() {
        // Show choice dialog for photo or video
        val options = arrayOf("Photo", "Video")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Media Type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> openVideoPicker()
                }
            }
            .show()
    }
    
    private fun openImagePicker() {
        isVideoMode = false
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = "image/*"
        }
        pickImagesLauncher.launch(Intent.createChooser(intent, "Select Photos"))
    }
    
    private fun openVideoPicker() {
        isVideoMode = true
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).apply {
            type = "video/*"
        }
        pickVideoLauncher.launch(Intent.createChooser(intent, "Select Video"))
    }
    
    private fun processSelectedVideo(videoUri: Uri) {
        selectedVideoUri = videoUri
        
        // Clear any previously selected images
        selectedBitmaps.clear()
        viewModel.clearImages()
        
        // Extract thumbnail
        videoThumbnail = extractVideoThumbnail(videoUri)
        
        // Get video duration
        val duration = getVideoDuration(videoUri)
        val maxDuration = 60_000L // 60 seconds
        
        if (duration > maxDuration) {
            Toast.makeText(
                requireContext(),
                "Video must be 60 seconds or less. Your video is ${duration / 1000} seconds.",
                Toast.LENGTH_LONG
            ).show()
            selectedVideoUri = null
            return
        }
        
        // Show video preview
        updateVideoPreview()
    }
    
    private fun extractVideoThumbnail(videoUri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), videoUri)
            val bitmap = retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            bitmap
        } catch (e: Exception) {
            Log.e("AddPostFragment", "Failed to extract thumbnail", e)
            null
        }
    }
    
    private fun getVideoDuration(videoUri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(requireContext(), videoUri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun updateVideoPreview() {
        if (selectedVideoUri == null) {
            binding.selectImageButton.visibility = View.VISIBLE
            binding.postImage.visibility = View.GONE
            binding.imagePager.visibility = View.GONE
            return
        }
        
        binding.selectImageButton.visibility = View.GONE
        binding.imagePager.visibility = View.GONE
        binding.rvSelectedImages.visibility = View.GONE
        binding.dotsIndicator.visibility = View.GONE
        
        // Show thumbnail with play icon overlay
        binding.postImage.visibility = View.VISIBLE
        videoThumbnail?.let {
            binding.postImage.setImageBitmap(it)
        }
        
        // Show video indicator
        Toast.makeText(requireContext(), "Video selected! Tap Share to upload.", Toast.LENGTH_SHORT).show()
    }

    private fun processSelectedImages(uris: List<Uri>) {
        val newBitmaps = mutableListOf<Bitmap>()
        val newBytesList = mutableListOf<ByteArray>()

        for (uri in uris) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // Resize if too large (keep max 1200px on longest side)
                    val resized = resizeBitmap(bitmap, 1200)
                    newBitmaps.add(resized)

                    val stream = ByteArrayOutputStream()
                    resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    newBytesList.add(stream.toByteArray())
                }
            } catch (e: Exception) {
                // Skip failed images
            }
        }

        if (newBytesList.isNotEmpty()) {
            // Add to VM
            viewModel.addImages(newBytesList)

            // Add bitmaps for local preview
            selectedBitmaps.addAll(newBitmaps)
            // Limit to 10
            while (selectedBitmaps.size > 10) {
                selectedBitmaps.removeAt(selectedBitmaps.size - 1)
            }

            updateImagePreview()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun updateImagePreview() {
        if (selectedBitmaps.isEmpty()) {
            binding.imagePager.visibility = View.GONE
            binding.postImage.visibility = View.GONE
            binding.selectImageButton.visibility = View.VISIBLE
            binding.rvSelectedImages.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
            return
        }

        // Hide placeholder
        binding.selectImageButton.visibility = View.GONE

        if (selectedBitmaps.size == 1) {
            // Single image: use the ImageView directly
            binding.imagePager.visibility = View.GONE
            binding.postImage.visibility = View.VISIBLE
            binding.postImage.setImageBitmap(selectedBitmaps[0])
            binding.rvSelectedImages.visibility = View.GONE
            binding.dotsIndicator.visibility = View.GONE
        } else {
            // Multiple images: use ViewPager2 carousel
            binding.postImage.visibility = View.GONE
            binding.imagePager.visibility = View.VISIBLE
            carouselAdapter.submitBitmaps(selectedBitmaps)

            // Show thumbnail strip
            binding.rvSelectedImages.visibility = View.VISIBLE
            selectedImagesAdapter.submitList(selectedBitmaps)

            // Show dot indicators
            setupDots(selectedBitmaps.size)
        }
    }

    private fun removeImage(index: Int) {
        if (index in selectedBitmaps.indices) {
            selectedBitmaps.removeAt(index)
            viewModel.removeImage(index)
            updateImagePreview()
        }
    }

    // ====== Dot Indicators ======

    private fun setupDots(count: Int) {
        binding.dotsIndicator.removeAllViews()
        if (count <= 1) {
            binding.dotsIndicator.visibility = View.GONE
            return
        }
        binding.dotsIndicator.visibility = View.VISIBLE
        for (i in 0 until count) {
            val dot = ImageView(requireContext()).apply {
                val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 8
                layoutParams = LinearLayout.LayoutParams(
                    18, 18
                ).apply {
                    marginStart = 4
                    marginEnd = 4
                }
                setImageResource(
                    if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
            binding.dotsIndicator.addView(dot)
        }
    }

    private fun updateDots(selectedPos: Int) {
        for (i in 0 until binding.dotsIndicator.childCount) {
            val dot = binding.dotsIndicator.getChildAt(i) as? ImageView
            dot?.setImageResource(
                if (i == selectedPos) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    // ====== Location Dialog ======

    private fun showLocationDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter location"
            setText(viewModel.addPostUiState.value.location)
            setPadding(48, 32, 48, 32)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Location")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val location = editText.text.toString().trim()
                viewModel.setLocation(location)
            }
            .setNegativeButton("Remove") { _, _ ->
                viewModel.clearLocation()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    // ====== Music Picker ======

    private fun showMusicPicker() {
        val musicPicker = MusicPickerBottomSheet()
        val state = viewModel.addPostUiState.value
        if (state.musicName.isNotEmpty()) {
            musicPicker.setCurrentSelection(state.musicName, state.musicArtist)
        }
        musicPicker.setOnMusicSelectedListener { music ->
            viewModel.setMusic(music.name, music.artist)
        }
        musicPicker.show(childFragmentManager, MusicPickerBottomSheet.TAG)
    }

    // ====== Share Post ======

    private fun sharePost() {
        if (isVideoMode && selectedVideoUri != null) {
            // Video post - compress and upload
            shareVideoPost()
        } else if (selectedBitmaps.isNotEmpty()) {
            // Image post
            viewModel.createEnhancedPost()
        } else {
            Toast.makeText(requireContext(), "Please select an image or video", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareVideoPost() {
        val videoUri = selectedVideoUri ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Show loading
                binding.loadingOverlay.isVisible = true
                binding.share.isEnabled = false
                
                Toast.makeText(requireContext(), "Compressing video...", Toast.LENGTH_SHORT).show()
                
                // Compress video
                val result = VideoCompressorUtil.compressVideo(
                    context = requireContext(),
                    videoUri = videoUri,
                    quality = VideoQuality.MEDIUM,
                    onProgress = { progress ->
                        // Could update a progress bar here
                        Log.d("AddPostFragment", "Compression progress: $progress%")
                    }
                )
                
                Log.d("AddPostFragment", "Video compressed: ${result.compressedPath}")
                Log.d("AddPostFragment", "Compression ratio: ${(result.compressionRatio * 100).toInt()}%")
                
                // Create video post via ViewModel
                val thumbnailBytes = videoThumbnail?.let { bitmap ->
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    stream.toByteArray()
                }
                
                viewModel.createVideoPost(
                    videoPath = result.compressedPath,
                    thumbnailBytes = thumbnailBytes,
                    duration = getVideoDuration(videoUri).toInt()
                )
                
            } catch (e: Exception) {
                Log.e("AddPostFragment", "Video processing failed", e)
                Toast.makeText(requireContext(), "Video processing failed: ${e.message}", Toast.LENGTH_LONG).show()
                binding.loadingOverlay.isVisible = false
                binding.share.isEnabled = true
            }
        }
    }

    // ====== Reset Form ======

    private fun resetForm() {
        binding.captionInput.text?.clear()
        binding.postImage.setImageDrawable(null)
        binding.postImage.visibility = View.GONE
        binding.imagePager.visibility = View.GONE
        binding.selectImageButton.visibility = View.VISIBLE
        binding.rvSelectedImages.visibility = View.GONE
        binding.dotsIndicator.visibility = View.GONE
        selectedBitmaps.clear()
        
        // Reset video state
        selectedVideoUri = null
        videoThumbnail = null
        isVideoMode = false
        releaseVideoPlayer()
        
        viewModel.resetAddPostForm()
    }
    
    private fun releaseVideoPlayer() {
        videoPlayer?.release()
        videoPlayer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseVideoPlayer()
        _binding = null
    }
}
