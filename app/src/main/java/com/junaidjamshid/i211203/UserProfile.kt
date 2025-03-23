package com.junaidjamshid.i211203

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class UserProfile : AppCompatActivity() {

    private var userId: String? = null
    private lateinit var userNameText: TextView
    private lateinit var userBioText: TextView
    private lateinit var userFollowersCount: TextView
    private lateinit var userFollowingCount: TextView
    private lateinit var userPostsCount: TextView
    private lateinit var userProfileImage: ImageView
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        supportActionBar?.hide()

        // Initialize views
        userNameText = findViewById(R.id.user_name_text)
        userBioText = findViewById(R.id.user_bio_text)
        userFollowersCount = findViewById(R.id.user_followers_count)
        userFollowingCount = findViewById(R.id.user_following_count)
        userPostsCount = findViewById(R.id.user_posts_count)
        userProfileImage = findViewById(R.id.user_profile_image)

        // Setup back button
        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            finish()
        }

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID")

        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        realtimeDb.child("Users").child(userId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Extract user data
                    val username = snapshot.child("username").getValue(String::class.java) ?: "User Name"
                    val bio = snapshot.child("bio").getValue(String::class.java) ?: ""

                    // Update UI with user details
                    userNameText.text = username
                    if (bio.isNotEmpty()) {
                        userBioText.text = bio
                        userBioText.visibility = View.VISIBLE
                    } else {
                        userBioText.visibility = View.GONE
                    }

                    // Load profile image
                    loadProfileImage(snapshot)

                    // Load followers and following counts
                    loadFollowersCounts()

                    // Load posts count
                    loadPostsCount()
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadProfileImage(userSnapshot: com.google.firebase.database.DataSnapshot) {
        // If the image is stored as Base64 string
        val profileImageData = userSnapshot.child("profilePicture").getValue(String::class.java)
        if (!profileImageData.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(profileImageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                userProfileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                userProfileImage.setImageResource(R.drawable.default_profile)
            }
        } else {
            userProfileImage.setImageResource(R.drawable.default_profile)
        }
    }

    private fun loadFollowersCounts() {
        // Get followers count
        realtimeDb.child("followers").child(userId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    var count = 0
                    snapshot.children.forEach { _ -> count++ }
                    userFollowersCount.text = count.toString()
                } else {
                    userFollowersCount.text = "0"
                }
            }
            .addOnFailureListener {
                userFollowersCount.text = "0"
            }

        // Get following count
        realtimeDb.child("following").child(userId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    var count = 0
                    snapshot.children.forEach { _ -> count++ }
                    userFollowingCount.text = count.toString()
                } else {
                    userFollowingCount.text = "0"
                }
            }
            .addOnFailureListener {
                userFollowingCount.text = "0"
            }
    }

    private fun loadPostsCount() {
        realtimeDb.child("posts")
            .orderByChild("userId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.childrenCount.toInt()
                userPostsCount.text = count.toString()

                // If no posts, show empty state
                if (count == 0) {
                    findViewById<View>(R.id.posts_recycler_view).visibility = View.GONE
                    findViewById<View>(R.id.empty_posts_view).visibility = View.VISIBLE
                } else {
                    findViewById<View>(R.id.posts_recycler_view).visibility = View.VISIBLE
                    findViewById<View>(R.id.empty_posts_view).visibility = View.GONE
                }
            }
            .addOnFailureListener {
                userPostsCount.text = "0"
                findViewById<View>(R.id.posts_recycler_view).visibility = View.GONE
                findViewById<View>(R.id.empty_posts_view).visibility = View.VISIBLE
            }
    }
}