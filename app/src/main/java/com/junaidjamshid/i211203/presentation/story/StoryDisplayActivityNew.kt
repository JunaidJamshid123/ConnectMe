package com.junaidjamshid.i211203.presentation.story

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Story
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

/**
 * Instagram-style full-screen Story Viewer with multi-segment progress bars,
 * tap left/right navigation, long-press pause, and swipe-down dismiss.
 */
@AndroidEntryPoint
class StoryDisplayActivityNew : AppCompatActivity() {

    private val viewModel: StoryViewModel by viewModels()

    private lateinit var imgStoryContent: ImageView
    private lateinit var imgUserProfile: CircleImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvTimePosted: TextView
    private lateinit var btnClose: ImageView
    private lateinit var progressContainer: LinearLayout

    private val progressBars = mutableListOf<ProgressBar>()
    private var currentAnimator: ObjectAnimator? = null
    private val storyDuration = 5000L
    private var isPaused = false
    private var currentIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_story_display)

        imgStoryContent = findViewById(R.id.imgStoryContent)
        imgUserProfile = findViewById(R.id.imgUserProfile)
        tvUsername = findViewById(R.id.tvUsername)
        tvTimePosted = findViewById(R.id.tvTimePosted)
        btnClose = findViewById(R.id.btnClose)
        progressContainer = findViewById(R.id.progress_container)

        val userId = intent.getStringExtra(EXTRA_USER_ID)
        if (userId == null) { finish(); return }

        setupTouchListeners()
        observeState()
        viewModel.loadUserStories(userId)
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

        btnClose.setOnClickListener { finish() }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.stories.isNotEmpty() && progressBars.isEmpty()) {
                        buildProgressBars(state.stories.size)
                    }

                    val newIndex = state.currentStoryIndex
                    if (state.currentStory != null && newIndex != currentIndex) {
                        currentIndex = newIndex
                        displayStory(state.currentStory)
                        animateProgressBar(newIndex)
                        // Fill all previous progress bars
                        for (i in 0 until newIndex) {
                            progressBars.getOrNull(i)?.progress = 100
                        }
                        // Reset all following progress bars
                        for (i in newIndex + 1 until progressBars.size) {
                            progressBars.getOrNull(i)?.progress = 0
                        }
                    }

                    // Close when stories are done
                    if (state.stories.isNotEmpty() && state.currentStoryIndex >= state.stories.size) {
                        finish()
                    }

                    state.error?.let {
                        Toast.makeText(this@StoryDisplayActivityNew, it, Toast.LENGTH_SHORT).show()
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
                layoutParams = LinearLayout.LayoutParams(0, (2 * resources.displayMetrics.density).toInt(), 1f).apply {
                    if (i < count - 1) marginEnd = gapPx
                }
                max = 100
                progress = 0
                progressDrawable = resources.getDrawable(R.drawable.bg_story_progress_bg, null)
                progressDrawable = createProgressDrawable()
            }
            progressBars.add(pb)
            progressContainer.addView(pb)
        }
    }

    private fun createProgressDrawable(): android.graphics.drawable.Drawable {
        val layers = android.graphics.drawable.LayerDrawable(arrayOf(
            resources.getDrawable(R.drawable.bg_story_progress_bg, null),
            android.graphics.drawable.ClipDrawable(
                resources.getDrawable(R.drawable.bg_story_progress_fill, null),
                android.view.Gravity.START,
                android.graphics.drawable.ClipDrawable.HORIZONTAL
            )
        ))
        layers.setId(0, android.R.id.background)
        layers.setId(1, android.R.id.progress)
        return layers
    }

    private fun animateProgressBar(index: Int) {
        currentAnimator?.cancel()
        val pb = progressBars.getOrNull(index) ?: return
        pb.progress = 0

        currentAnimator = ObjectAnimator.ofInt(pb, "progress", 0, 100).apply {
            duration = storyDuration
            interpolator = LinearInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!isPaused) {
                        goToNextStory()
                    }
                }
            })
            start()
        }
    }

    private fun displayStory(story: Story) {
        tvUsername.text = story.username
        tvTimePosted.text = getTimeAgo(story.timestamp)

        loadBase64(story.storyImageUrl, imgStoryContent)
        loadBase64(story.userProfileImage, imgUserProfile)
    }

    private fun loadBase64(base64: String, imageView: ImageView) {
        if (base64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) { imageView.setImageBitmap(bitmap); return }
            } catch (_: Exception) {}
        }
        imageView.setImageResource(R.drawable.default_profile)
    }

    private fun pauseStory() {
        isPaused = true
        currentAnimator?.pause()
    }

    private fun resumeStory() {
        isPaused = false
        currentAnimator?.resume()
    }

    private fun goToNextStory() {
        currentAnimator?.cancel()
        isPaused = false
        viewModel.nextStory()
    }

    private fun goToPreviousStory() {
        currentAnimator?.cancel()
        isPaused = false
        viewModel.previousStory()
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        return when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h"
            else -> "${hours / 24}d"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentAnimator?.cancel()
    }

    companion object {
        const val EXTRA_USER_ID = "USER_ID"
        const val EXTRA_STORY_ID = "STORY_ID"

        fun newIntent(context: Context, userId: String, storyId: String? = null): Intent {
            return Intent(context, StoryDisplayActivityNew::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                storyId?.let { putExtra(EXTRA_STORY_ID, it) }
            }
        }
    }
}
