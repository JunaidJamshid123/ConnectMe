package com.junaidjamshid.i211203

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.junaidjamshid.i211203.models.Story
import java.text.SimpleDateFormat
import java.util.*

class StoryDisplayActivity : AppCompatActivity() {

    private val TAG = "StoryDisplayActivity"

    // UI Components
    private lateinit var imgStoryContent: ImageView
    private lateinit var imgUserProfile: ShapeableImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvTimePosted: TextView
    private lateinit var tvCaption: TextView
    private lateinit var storyProgressBar: ProgressBar
    private lateinit var btnClose: ImageView
    private lateinit var leftTouchArea: View
    private lateinit var rightTouchArea: View

    // Data
    private var stories = mutableListOf<Story>()
    private var currentStoryPosition = 0
    private var storyDuration = 5000L // 5 seconds per story

    // Timer for auto progression
    private var countDownTimer: CountDownTimer? = null
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_display)

        // Initialize UI components
        imgStoryContent = findViewById(R.id.imgStoryContent)
        imgUserProfile = findViewById(R.id.imgUserProfile)
        tvUsername = findViewById(R.id.tvUsername)
        tvTimePosted = findViewById(R.id.tvTimePosted)
        tvCaption = findViewById(R.id.tvCaption)
        storyProgressBar = findViewById(R.id.storyProgressBar)
        btnClose = findViewById(R.id.btnClose)
        leftTouchArea = findViewById(R.id.leftTouchArea)
        rightTouchArea = findViewById(R.id.rightTouchArea)

        // Get data from intent
        val storyId = intent.getStringExtra("storyId") ?: ""
        val userId = intent.getStringExtra("userId") ?: ""

        // Setup click listeners
        setupClickListeners()

        // Load stories for this user
        loadUserStories(userId, storyId)
    }

    private fun setupClickListeners() {
        // Close button
        btnClose.setOnClickListener {
            finish()
        }

        // Touch areas for navigation
        leftTouchArea.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pauseStory()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    resumeStory()
                    showPreviousStory()
                    true
                }
                else -> false
            }
        }

        rightTouchArea.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pauseStory()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    resumeStory()
                    showNextStory()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserStories(userId: String, initialStoryId: String) {
        val storiesRef = FirebaseDatabase.getInstance().getReference("stories")

        // Query stories by user ID
        storiesRef.orderByChild("userId").equalTo(userId).get()
            .addOnSuccessListener { snapshot ->
                stories.clear()

                for (storySnapshot in snapshot.children) {
                    try {
                        val story = storySnapshot.getValue(Story::class.java)
                        story?.let {
                            // Only add stories that haven't expired
                            val currentTime = System.currentTimeMillis()
                            if (it.expiryTimestamp > currentTime) {
                                stories.add(it)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing story: ${e.message}")
                    }
                }

                // Sort stories by timestamp (newest first)
                stories.sortByDescending { it.timestamp }

                if (stories.isNotEmpty()) {
                    // Find the initial story position if a specific story ID was provided
                    if (initialStoryId.isNotEmpty()) {
                        val position = stories.indexOfFirst { it.storyId == initialStoryId }
                        if (position != -1) {
                            currentStoryPosition = position
                        }
                    }

                    // Display the story
                    displayStory(currentStoryPosition)

                    // Mark story as viewed
                    markStoryAsViewed(stories[currentStoryPosition])
                } else {
                    Toast.makeText(this, "No stories available", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading stories: ${e.message}")
                Toast.makeText(this, "Failed to load stories", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun displayStory(position: Int) {
        if (position < 0 || position >= stories.size) {
            finish()
            return
        }

        val story = stories[position]

        // Update UI with story data
        tvUsername.text = story.username
        tvTimePosted.text = getTimeAgo(story.timestamp)
        tvCaption.text = story.caption

        // Load profile image
        if (story.userProfileImage.isNotEmpty()) {
            val profileBitmap = decodeBase64Image(story.userProfileImage)
            profileBitmap?.let {
                imgUserProfile.setImageBitmap(it)
            } ?: run {
                imgUserProfile.setImageResource(R.drawable.junaid1)
            }
        } else {
            imgUserProfile.setImageResource(R.drawable.junaid1)
        }

        // Load story image
        if (story.storyImageUrl.isNotEmpty()) {
            val storyBitmap = decodeBase64Image(story.storyImageUrl)
            storyBitmap?.let {
                imgStoryContent.setImageBitmap(it)
            } ?: run {
                // Show placeholder or error image
                imgStoryContent.setImageResource(R.drawable.junaid1)
            }
        } else {
            // Show placeholder or error image
            imgStoryContent.setImageResource(R.drawable.junaid1)
        }

        // Start progress bar
        startProgressBar()
    }

    private fun startProgressBar() {
        // Cancel any existing timer
        countDownTimer?.cancel()

        // Reset progress
        storyProgressBar.progress = 0

        // Create new timer
        countDownTimer = object : CountDownTimer(storyDuration, 50) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isPaused) {
                    val progress = ((storyDuration - millisUntilFinished) * 100 / storyDuration).toInt()
                    storyProgressBar.progress = progress
                }
            }

            override fun onFinish() {
                storyProgressBar.progress = 100
                showNextStory()
            }
        }.start()
    }

    private fun pauseStory() {
        isPaused = true
    }

    private fun resumeStory() {
        isPaused = false
    }

    private fun showPreviousStory() {
        if (currentStoryPosition > 0) {
            currentStoryPosition--
            displayStory(currentStoryPosition)
            markStoryAsViewed(stories[currentStoryPosition])
        } else {
            // No more previous stories, go back to previous user's stories or finish
            finish()
        }
    }

    private fun showNextStory() {
        if (currentStoryPosition < stories.size - 1) {
            currentStoryPosition++
            displayStory(currentStoryPosition)
            markStoryAsViewed(stories[currentStoryPosition])
        } else {
            // No more stories, finish activity
            finish()
        }
    }

    private fun markStoryAsViewed(story: Story) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = currentUser.uid

        // Don't mark as viewed if it's the user's own story
        if (story.userId == currentUserId) {
            return
        }

        // Check if user already viewed the story
        if (!story.viewers.contains(currentUserId)) {
            // Add user to viewers list
            story.viewers.add(currentUserId)

            // Update in Firebase
            val storyRef = FirebaseDatabase.getInstance().getReference("stories")
                .child(story.storyId).child("viewers")

            storyRef.setValue(story.viewers)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error marking story as viewed: ${e.message}")
                }
        }
    }

    // Helper function to decode Base64 string to Bitmap
    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64 image: ${e.message}")
            null
        }
    }

    // Helper function to format timestamp as "time ago"
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (!isPaused && stories.isNotEmpty()) {
            startProgressBar()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}