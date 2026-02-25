package com.junaidjamshid.i211203.presentation.story

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.databinding.ActivityStoryDisplayBinding
import com.junaidjamshid.i211203.domain.model.Story
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Clean Architecture Story Display Activity.
 */
@AndroidEntryPoint
class StoryDisplayActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityStoryDisplayBinding
    private val viewModel: StoryViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = ActivityStoryDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val userId = intent.getStringExtra("USER_ID")
        val storyId = intent.getStringExtra("STORY_ID")
        
        if (userId != null) {
            setupTouchListeners()
            observeUiState()
            viewModel.loadUserStories(userId)
        } else {
            Toast.makeText(this, "No story data", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupTouchListeners() {
        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    viewModel.pauseProgress()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val screenWidth = binding.root.width
                    val touchX = event.x
                    
                    if (touchX < screenWidth / 2) {
                        viewModel.previousStory()
                    } else {
                        viewModel.nextStory()
                    }
                    true
                }
                else -> false
            }
        }
        
        binding.btnClose.setOnClickListener {
            finish()
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
    
    private fun handleUiState(state: StoryUiState) {
        // Update progress bar
        binding.storyProgressBar.progress = (state.storyProgress * 100).toInt()
        
        // Update current story
        state.currentStory?.let { story ->
            displayStory(story)
        }
        
        // Close if no more stories
        if (state.stories.isNotEmpty() && 
            state.currentStoryIndex >= state.stories.size) {
            finish()
        }
        
        // Handle error
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun displayStory(story: Story) {
        binding.tvUsername.text = story.username
        binding.tvTimePosted.text = getTimeAgo(story.timestamp)
        
        // Load story image
        if (story.storyImageUrl.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(story.storyImageUrl, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.imgStoryContent.setImageBitmap(bitmap)
            } catch (e: Exception) {
                binding.imgStoryContent.setImageResource(R.drawable.junaid1)
            }
        }
        
        // Load profile image
        if (story.userProfileImage.isNotEmpty()) {
            try {
                val imageBytes = android.util.Base64.decode(story.userProfileImage, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.imgUserProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                binding.imgUserProfile.setImageResource(R.drawable.junaid1)
            }
        }
    }
    
    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        
        return when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h ago"
            else -> "${hours / 24}d ago"
        }
    }
}
