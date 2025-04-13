package com.junaidjamshid.i211203

import android.os.Bundle
import android.util.Base64
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.models.Post
import de.hdodenhof.circleimageview.CircleImageView

class PostDetailActivity : AppCompatActivity() {

    private lateinit var postId: String
    private lateinit var userProfileImage: CircleImageView
    private lateinit var username: TextView
    private lateinit var postImage: ImageView
    private lateinit var likeButton: ImageView
    private lateinit var commentButton: ImageView
    private lateinit var likes: TextView
    private lateinit var publisher: TextView
    private lateinit var description: TextView
    private lateinit var comments: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        // Get post ID from intent
        supportActionBar?.hide()
        postId = intent.getStringExtra("postId") ?: ""
        if (postId.isEmpty()) {
            Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initializeViews()

        // Set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        supportActionBar?.title = "Post Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Load post details
        loadPostDetails()
    }

    private fun initializeViews() {
        userProfileImage = findViewById(R.id.user_profile_image)
        username = findViewById(R.id.username)
        postImage = findViewById(R.id.post_image)
        likeButton = findViewById(R.id.like_button)
        commentButton = findViewById(R.id.comment_button)
        likes = findViewById(R.id.likes)
        publisher = findViewById(R.id.publisher)
        description = findViewById(R.id.description)
        comments = findViewById(R.id.comments)
    }

    private fun loadPostDetails() {
        val postRef = FirebaseDatabase.getInstance().reference
            .child("posts")
            .child(postId)

        postRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val post = snapshot.getValue(Post::class.java)
                        post?.let {
                            // Set username and profile image
                            username.text = it.username
                            publisher.text = it.username
                            description.text = it.caption

                            // Set like count
                            val likesCount = it.likes.size
                            likes.text = "$likesCount likes"

                            // Set comment count
                            val commentsCount = it.comments.size
                            comments.text = "View all $commentsCount comments"

                            // Load user profile image (from Base64 string)
                            if (it.userProfileImage.isNotEmpty()) {
                                try {
                                    val profileImageBytes = Base64.decode(it.userProfileImage, Base64.DEFAULT)
                                    val profileBitmap = BitmapFactory.decodeByteArray(
                                        profileImageBytes, 0, profileImageBytes.size
                                    )
                                    userProfileImage.setImageBitmap(profileBitmap)
                                } catch (e: Exception) {
                                    userProfileImage.setImageResource(R.drawable.avatar)
                                }
                            } else {
                                userProfileImage.setImageResource(R.drawable.avatar)
                            }

                            // Load post image (from Base64 string)
                            if (it.postImageUrl.isNotEmpty()) {
                                try {
                                    val imageBytes = Base64.decode(it.postImageUrl, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(
                                        imageBytes, 0, imageBytes.size
                                    )
                                    postImage.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    postImage.setImageResource(R.drawable.avatar)
                                    Toast.makeText(
                                        this@PostDetailActivity,
                                        "Error loading image: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                postImage.setImageResource(R.drawable.avatar)
                            }

                            // Set like button click listener
                            setupLikeButton(it)

                            // Set comment button click listener
                            setupCommentButton(it)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@PostDetailActivity,
                            "Error parsing post data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@PostDetailActivity,
                        "Post not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PostDetailActivity,
                    "Error loading post: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupLikeButton(post: Post) {
        // Implementation for like functionality
        likeButton.setOnClickListener {
            Toast.makeText(this, "Like functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCommentButton(post: Post) {
        // Implementation for comment functionality
        commentButton.setOnClickListener {
            Toast.makeText(this, "Comment functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}