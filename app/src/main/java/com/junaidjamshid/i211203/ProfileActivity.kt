package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    // UI elements
    private lateinit var profileImage: CircleImageView
    private lateinit var usernameText: TextView
    private lateinit var bioText: TextView
    private lateinit var postsCountText: TextView
    private lateinit var followersCountText: TextView
    private lateinit var followingCountText: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize UI elements
        profileImage = findViewById(R.id.profile_image)
        usernameText = findViewById(R.id.username_text)
        bioText = findViewById(R.id.bio_text)
        postsCountText = findViewById(R.id.posts_count)
        followersCountText = findViewById(R.id.followers_count)
        followingCountText = findViewById(R.id.following_count)

        // Set default values
        setDefaultValues()

        // Initialize grid view for thumbnails
        val gridView = findViewById<GridView>(R.id.grid_view)

        // Temporary image list - this will be replaced with actual user posts later
        val imageList = listOf(
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1,
            R.drawable.junaid2,
            R.drawable.junaid1
        )

        val adapter = ImageAdapter(this, imageList)
        gridView.adapter = adapter

        // Set up button click listeners
        setupClickListeners()

        // Load user profile data
        loadUserProfile()
    }

    private fun setDefaultValues() {
        // Set default avatar
        profileImage.setImageResource(R.drawable.junaid1)

        // Set default text values
        bioText.text = "No Bio"
        postsCountText.text = "0"
        followersCountText.text = "0"
        followingCountText.text = "0"
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // User not logged in, redirect to login screen
            startActivity(Intent(this, LoginScreem::class.java))
            finish()
            return
        }

        val userId = currentUser.uid

        // Reference to the user's data in Firebase
        val userRef = database.child("Users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get user data
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Username"
                    val fullName = snapshot.child("fullName").getValue(String::class.java) ?: "Full Name"
                    val bio = snapshot.child("bio").getValue(String::class.java)
                    val profilePictureUrl = snapshot.child("profilePictureUrl").getValue(String::class.java)

                    // Update UI with user data
                    usernameText.text = fullName

                    // Set bio text - if bio is null or empty, show "No Bio"
                    bioText.text = if (!bio.isNullOrEmpty()) bio else "No Bio"

                    // Load profile image if available, otherwise use default avatar
                    if (!profilePictureUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(profilePictureUrl)
                            .placeholder(R.drawable.junaid1)
                            .error(R.drawable.junaid1)
                            .into(profileImage)
                    } else {
                        // Use default avatar
                        profileImage.setImageResource(R.drawable.junaid1)
                    }

                    // Handle followers and following counts
                    val followersSnapshot = snapshot.child("followers")
                    val followingSnapshot = snapshot.child("following")

                    if (followersSnapshot.exists()) {
                        val followers = followersSnapshot.childrenCount
                        followersCountText.text = followers.toString()
                    } else {
                        followersCountText.text = "0"
                    }

                    if (followingSnapshot.exists()) {
                        val following = followingSnapshot.childrenCount
                        followingCountText.text = following.toString()
                    } else {
                        followingCountText.text = "0"
                    }

                    // For posts count
                    countUserPosts(userId)
                } else {
                    // If user data doesn't exist, keep default values
                    Toast.makeText(this@ProfileActivity, "User profile not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun countUserPosts(userId: String) {
        // Assuming you'll store posts in a structure like "Posts" -> "userId" -> "postId"
        val postsRef = database.child("Posts").child(userId)

        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val postsCount = snapshot.childrenCount
                    postsCountText.text = postsCount.toString()
                } else {
                    // No posts found
                    postsCountText.text = "0"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // If there's an error, show 0 posts
                postsCountText.text = "0"
            }
        })
    }

    private fun setupClickListeners() {
        val home = findViewById<LinearLayout>(R.id.Home)
        val search = findViewById<LinearLayout>(R.id.Search)
        val newPost = findViewById<LinearLayout>(R.id.NewPost)
        val profile = findViewById<LinearLayout>(R.id.Profile)
        val contacts = findViewById<LinearLayout>(R.id.Contacts)
        val followers_btn = findViewById<LinearLayout>(R.id.followers)
        val following_btn = findViewById<LinearLayout>(R.id.following)
        val edit = findViewById<ImageView>(R.id.edit_profile)
        val logout = findViewById<ImageView>(R.id.logout)

        // Set up logout click listener
        logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        edit.setOnClickListener {
            val intent = Intent(this, EditProfile::class.java)
            startActivity(intent)
        }

        following_btn.setOnClickListener {
            val intent = Intent(this, Following::class.java)
            startActivity(intent)
        }

        followers_btn.setOnClickListener {
            val intent = Intent(this, Followers::class.java)
            startActivity(intent)
        }

        home.setOnClickListener {
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
        }

        search.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        newPost.setOnClickListener {
            val intent = Intent(this, NewPost::class.java)
            startActivity(intent)
        }

        contacts.setOnClickListener {
            val intent = Intent(this, ContactsPage::class.java)
            startActivity(intent)
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to logout?")

        builder.setPositiveButton("Yes") { _, _ ->
            // Perform logout operation
            performLogout()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            // Dismiss the dialog
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun performLogout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Navigate to login screen
        val intent = Intent(this, LoginScreem::class.java)
        // Clear the back stack so user can't go back after logout
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}