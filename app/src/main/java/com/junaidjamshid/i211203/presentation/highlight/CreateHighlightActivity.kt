package com.junaidjamshid.i211203.presentation.highlight

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.repository.StoryRepository
import com.junaidjamshid.i211203.presentation.highlight.adapter.SelectableStoryAdapter
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

/**
 * Activity for creating a new story highlight.
 * Users can select stories from their current stories and give the highlight a name.
 */
@AndroidEntryPoint
class CreateHighlightActivity : AppCompatActivity() {

    @Inject
    lateinit var storyRepository: StoryRepository

    private val viewModel: HighlightViewModel by viewModels()

    private lateinit var toolbar: Toolbar
    private lateinit var btnDone: MaterialButton
    private lateinit var imgCover: CircleImageView
    private lateinit var btnEditCover: ImageView
    private lateinit var etHighlightName: TextInputEditText
    private lateinit var tvSelectedCount: Chip
    private lateinit var rvStories: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var loadingOverlay: FrameLayout
    private var btnAddMoreFromGallery: MaterialButton? = null

    private lateinit var adapter: SelectableStoryAdapter
    private val selectedStories = mutableSetOf<String>()
    private val galleryImages = mutableListOf<SelectableStory>() // For gallery-picked images
    private var coverImageBytes: ByteArray? = null
    private var userId: String? = null

    private val pickCoverImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleCoverImageSelection(it) }
    }
    
    private val pickGalleryImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        handleGalleryImagesSelection(uris)
    }

    companion object {
        private const val EXTRA_USER_ID = "user_id"

        fun newIntent(context: Context, userId: String): Intent {
            return Intent(context, CreateHighlightActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_highlight)

        userId = intent.getStringExtra(EXTRA_USER_ID)
        if (userId.isNullOrEmpty()) {
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        observeState()
        loadUserStories()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnDone = findViewById(R.id.btnDone)
        imgCover = findViewById(R.id.imgCover)
        btnEditCover = findViewById(R.id.btnEditCover)
        etHighlightName = findViewById(R.id.etHighlightName)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        rvStories = findViewById(R.id.rvStories)
        emptyState = findViewById(R.id.emptyState)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        btnAddMoreFromGallery = findViewById(R.id.btnAddMoreFromGallery)

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SelectableStoryAdapter { story ->
            toggleStorySelection(story)
        }

        rvStories.apply {
            layoutManager = GridLayoutManager(this@CreateHighlightActivity, 3)
            adapter = this@CreateHighlightActivity.adapter
        }
    }

    private fun setupClickListeners() {
        btnEditCover.setOnClickListener {
            pickCoverImage.launch("image/*")
        }

        imgCover.setOnClickListener {
            pickCoverImage.launch("image/*")
        }

        btnDone.setOnClickListener {
            createHighlight()
        }
        
        // Find and setup the add from gallery button in empty state
        emptyState.findViewById<View>(R.id.btnAddFromGallery)?.setOnClickListener {
            pickGalleryImages.launch("image/*")
        }
        
        // Add from gallery button in main view
        btnAddMoreFromGallery?.setOnClickListener {
            pickGalleryImages.launch("image/*")
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    if (state.highlightCreated) {
                        Toast.makeText(this@CreateHighlightActivity, "Highlight created!", Toast.LENGTH_SHORT).show()
                        viewModel.resetHighlightCreated()
                        setResult(RESULT_OK)
                        finish()
                    }

                    state.error?.let {
                        Toast.makeText(this@CreateHighlightActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun loadUserStories() {
        loadingOverlay.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = storyRepository.getUserStories(userId!!)
            loadingOverlay.visibility = View.GONE
            
            when (result) {
                is Resource.Success -> {
                    val stories = result.data ?: emptyList()
                    if (stories.isEmpty()) {
                        showEmptyState()
                    } else {
                        rvStories.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                        
                        val selectableStories = stories.map { story ->
                            SelectableStory(
                                id = story.storyId,
                                imageUrl = story.storyImageUrl,
                                isSelected = false
                            )
                        }
                        adapter.submitList(selectableStories)
                        
                        // Set first story as default cover
                        if (stories.isNotEmpty()) {
                            Glide.with(this@CreateHighlightActivity)
                                .load(stories.first().storyImageUrl)
                                .into(imgCover)
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this@CreateHighlightActivity, result.message, Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
                else -> {}
            }
        }
    }
    
    private fun showEmptyState() {
        if (galleryImages.isEmpty()) {
            rvStories.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvStories.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            adapter.submitList(galleryImages.toList())
        }
    }
    
    private fun handleGalleryImagesSelection(uris: List<Uri>) {
        if (uris.isEmpty()) return
        
        // Convert URIs to SelectableStory objects
        val newStories = uris.map { uri ->
            SelectableStory(
                id = UUID.randomUUID().toString(),
                imageUrl = uri.toString(),
                isSelected = true  // Auto-select newly added images
            )
        }
        
        galleryImages.addAll(newStories)
        newStories.forEach { selectedStories.add(it.id) }
        
        // Show the grid with gallery images
        rvStories.visibility = View.VISIBLE
        emptyState.visibility = View.GONE
        
        // Combine existing adapter list with new images
        val currentList = adapter.currentList.toMutableList()
        currentList.addAll(newStories)
        adapter.submitList(currentList)
        
        // Update selected count
        tvSelectedCount.text = "${selectedStories.size} selected"
        
        // Set first selected as cover if no cover set
        if (coverImageBytes == null && uris.isNotEmpty()) {
            Glide.with(this)
                .load(uris.first())
                .into(imgCover)
        }
    }

    private fun toggleStorySelection(story: SelectableStory) {
        if (selectedStories.contains(story.id)) {
            selectedStories.remove(story.id)
        } else {
            selectedStories.add(story.id)
        }

        // Update selection order in adapter
        adapter.updateSelectionOrder(selectedStories)

        // Update the adapter with new selection state
        val currentList = adapter.currentList.map { s ->
            s.copy(isSelected = selectedStories.contains(s.id))
        }
        adapter.submitList(currentList)

        // Update selected count with animation
        val count = selectedStories.size
        tvSelectedCount.text = if (count == 0) "0 selected" else "$count selected"

        // Update cover image to first selected story if no custom cover
        if (coverImageBytes == null && selectedStories.isNotEmpty()) {
            val firstSelected = currentList.find { it.isSelected }
            firstSelected?.let {
                Glide.with(this)
                    .load(it.imageUrl)
                    .into(imgCover)
            }
        }
    }

    private fun handleCoverImageSelection(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            coverImageBytes = stream.toByteArray()

            Glide.with(this)
                .load(uri)
                .into(imgCover)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createHighlight() {
        val name = etHighlightName.text.toString().trim()
        
        if (name.isEmpty()) {
            etHighlightName.error = "Please enter a name"
            return
        }

        if (selectedStories.isEmpty()) {
            Toast.makeText(this, "Please select at least one story", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the story URLs for selected stories
        val selectedUrls = adapter.currentList
            .filter { selectedStories.contains(it.id) }
            .map { it.imageUrl }

        viewModel.createHighlight(
            name = name,
            coverImageBytes = coverImageBytes,
            storyImageUrls = selectedUrls
        )
    }
}
