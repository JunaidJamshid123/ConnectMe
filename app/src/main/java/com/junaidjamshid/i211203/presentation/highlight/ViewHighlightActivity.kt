package com.junaidjamshid.i211203.presentation.highlight

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.HighlightStory
import com.junaidjamshid.i211203.domain.model.StoryHighlight
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

/**
 * Activity for viewing a story highlight with Instagram-style navigation.
 */
@AndroidEntryPoint
class ViewHighlightActivity : AppCompatActivity() {

    private val viewModel: HighlightViewModel by viewModels()

    private lateinit var imgStoryContent: ImageView
    private lateinit var imgHighlightCover: CircleImageView
    private lateinit var tvHighlightName: TextView
    private lateinit var btnClose: ImageView
    private lateinit var btnMore: ImageView
    private lateinit var progressContainer: LinearLayout
    private lateinit var progressLoading: ProgressBar

    private val progressBars = mutableListOf<ProgressBar>()
    private var currentAnimator: ObjectAnimator? = null
    private val storyDuration = 5000L
    private var isPaused = false
    private var currentIndex = -1

    companion object {
        private const val EXTRA_HIGHLIGHT_ID = "highlight_id"
        private const val EXTRA_HIGHLIGHT = "highlight"
        private const val EXTRA_USER_ID = "user_id"
        private const val EXTRA_IS_OWNER = "is_owner"

        fun newIntent(
            context: Context,
            highlightId: String,
            userId: String,
            isOwner: Boolean = false
        ): Intent {
            return Intent(context, ViewHighlightActivity::class.java).apply {
                putExtra(EXTRA_HIGHLIGHT_ID, highlightId)
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_IS_OWNER, isOwner)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_view_highlight)

        initViews()
        setupTouchListeners()
        observeState()

        val highlightId = intent.getStringExtra(EXTRA_HIGHLIGHT_ID)
        if (highlightId.isNullOrEmpty()) {
            finish()
            return
        }

        viewModel.loadHighlight(highlightId)
    }

    private fun initViews() {
        imgStoryContent = findViewById(R.id.imgStoryContent)
        imgHighlightCover = findViewById(R.id.imgHighlightCover)
        tvHighlightName = findViewById(R.id.tvHighlightName)
        btnClose = findViewById(R.id.btnClose)
        btnMore = findViewById(R.id.btnMore)
        progressContainer = findViewById(R.id.progress_container)
        progressLoading = findViewById(R.id.progressLoading)

        btnClose.setOnClickListener { finish() }
        
        val isOwner = intent.getBooleanExtra(EXTRA_IS_OWNER, false)
        btnMore.visibility = if (isOwner) View.VISIBLE else View.GONE
        btnMore.setOnClickListener { showMoreOptions() }
    }

    private fun setupTouchListeners() {
        val handler = Handler(Looper.getMainLooper())
        var isLongPress = false

        val longPressRunnable = Runnable {
            isLongPress = true
            pauseStory()
        }

        imgStoryContent.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPress = false
                    handler.postDelayed(longPressRunnable, 200)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (isLongPress) {
                        resumeStory()
                    } else {
                        val screenWidth = imgStoryContent.width
                        if (event.x < screenWidth / 3f) {
                            goToPreviousStory()
                        } else {
                            goToNextStory()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Show loading
                    progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Setup highlight info
                    state.currentHighlight?.let { highlight ->
                        tvHighlightName.text = highlight.name
                        
                        if (highlight.coverImageUrl.isNotEmpty()) {
                            Glide.with(this@ViewHighlightActivity)
                                .load(highlight.coverImageUrl)
                                .into(imgHighlightCover)
                        }

                        // Build progress bars if needed
                        if (highlight.stories.isNotEmpty() && progressBars.isEmpty()) {
                            buildProgressBars(highlight.stories.size)
                        }
                    }

                    // Display current story
                    val newIndex = state.currentStoryIndex
                    if (state.currentStory != null && newIndex != currentIndex) {
                        currentIndex = newIndex
                        displayStory(state.currentStory)
                        animateProgressBar(newIndex)
                        
                        // Fill previous progress bars
                        for (i in 0 until newIndex) {
                            progressBars.getOrNull(i)?.progress = 100
                        }
                        // Reset following progress bars
                        for (i in newIndex + 1 until progressBars.size) {
                            progressBars.getOrNull(i)?.progress = 0
                        }
                    }

                    // Close when stories are done
                    val stories = state.currentHighlight?.stories ?: emptyList()
                    if (stories.isNotEmpty() && state.currentStoryIndex >= stories.size) {
                        finish()
                    }

                    // Handle highlight deletion
                    if (state.highlightDeleted) {
                        Toast.makeText(this@ViewHighlightActivity, "Highlight deleted", Toast.LENGTH_SHORT).show()
                        viewModel.resetHighlightDeleted()
                        finish()
                    }

                    // Show errors
                    state.error?.let {
                        Toast.makeText(this@ViewHighlightActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun buildProgressBars(count: Int) {
        progressContainer.removeAllViews()
        progressBars.clear()
        val gapDp = 2
        val gapPx = (gapDp * resources.displayMetrics.density).toInt()

        for (i in 0 until count) {
            val pb = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (i < count - 1) marginEnd = gapPx
                }
                progressDrawable = resources.getDrawable(R.drawable.story_progress_drawable, theme)
                max = 100
                progress = 0
            }
            progressBars.add(pb)
            progressContainer.addView(pb)
        }
    }

    private fun displayStory(story: HighlightStory) {
        Glide.with(this)
            .load(story.storyImageUrl)
            .into(imgStoryContent)
    }

    private fun animateProgressBar(index: Int) {
        currentAnimator?.cancel()
        val pb = progressBars.getOrNull(index) ?: return
        pb.progress = 0

        currentAnimator = ObjectAnimator.ofInt(pb, "progress", 0, 100).apply {
            duration = storyDuration
            interpolator = LinearInterpolator()
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!isPaused) {
                        goToNextStory()
                    }
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }

    private fun pauseStory() {
        isPaused = true
        currentAnimator?.pause()
        viewModel.pauseStory()
    }

    private fun resumeStory() {
        isPaused = false
        currentAnimator?.resume()
        viewModel.resumeStory()
    }

    private fun goToNextStory() {
        val stories = viewModel.uiState.value.currentHighlight?.stories ?: return
        if (currentIndex < stories.size - 1) {
            viewModel.nextStory()
        } else {
            finish()
        }
    }

    private fun goToPreviousStory() {
        viewModel.previousStory()
    }

    private fun showMoreOptions() {
        val popup = PopupMenu(this, btnMore)
        popup.menuInflater.inflate(R.menu.menu_highlight_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_highlight -> {
                    showDeleteConfirmation()
                    true
                }
                R.id.action_edit_highlight -> {
                    val highlight = viewModel.uiState.value.currentHighlight
                    if (highlight != null) {
                        // TODO: Navigate to edit highlight screen
                        Toast.makeText(this, "Edit highlight: ${highlight.name}", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation() {
        val highlight = viewModel.uiState.value.currentHighlight ?: return
        
        AlertDialog.Builder(this)
            .setTitle("Delete Highlight")
            .setMessage("Are you sure you want to delete \"${highlight.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteHighlight(highlight.highlightId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentAnimator?.cancel()
    }
}
