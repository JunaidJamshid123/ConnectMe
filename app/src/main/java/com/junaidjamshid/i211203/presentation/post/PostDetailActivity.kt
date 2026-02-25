package com.junaidjamshid.i211203.presentation.post

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Comment
import com.junaidjamshid.i211203.presentation.profile.UserProfileActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Activity to display post details.
 * Clean Architecture implementation.
 */
@AndroidEntryPoint
class PostDetailActivity : AppCompatActivity() {
    
    private val viewModel: PostViewModel by viewModels()
    
    private lateinit var postImage: ImageView
    private lateinit var userProfileImage: ImageView
    private lateinit var usernameText: TextView
    private lateinit var captionText: TextView
    private lateinit var likesCountText: TextView
    private lateinit var likeButton: ImageView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    
    private var postId: String? = null
    
    companion object {
        private const val EXTRA_POST_ID = "post_id"
        
        fun newIntent(context: Context, postId: String): Intent {
            return Intent(context, PostDetailActivity::class.java).apply {
                putExtra(EXTRA_POST_ID, postId)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)
        
        postId = intent.getStringExtra(EXTRA_POST_ID)
        if (postId == null) {
            finish()
            return
        }
        
        setupViews()
        setupViews()
        setupClickListeners()
        observeState()
        
        viewModel.loadPost(postId!!)
    }
    
    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        postImage = findViewById(R.id.post_image)
        userProfileImage = findViewById(R.id.user_profile_image)
        usernameText = findViewById(R.id.username)
        captionText = findViewById(R.id.description)
        likesCountText = findViewById(R.id.likes)
        likeButton = findViewById(R.id.like_button)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }
    
    private fun setupClickListeners() {
        
        likeButton.setOnClickListener {
            postId?.let { id ->
                viewModel.toggleLike(id)
            }
        }
        
        userProfileImage.setOnClickListener {
            viewModel.uiState.value.post?.userId?.let { userId ->
                startActivity(UserProfileActivity.newIntent(this, userId))
            }
        }
        
        usernameText.setOnClickListener {
            viewModel.uiState.value.post?.userId?.let { userId ->
                startActivity(UserProfileActivity.newIntent(this, userId))
            }
        }
    }
    
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
    
    private fun updateUI(state: PostUiState) {
        // Update post details
        state.post?.let { post ->
            // Post image
            decodeBase64Image(post.postImageUrl)?.let { bitmap ->
                postImage.setImageBitmap(bitmap)
            }
            
            // User profile image
            post.userProfileImage?.let { profilePic ->
                decodeBase64Image(profilePic)?.let { bitmap ->
                    userProfileImage.setImageBitmap(bitmap)
                }
            }
            
            // Username and caption
            usernameText.text = post.username
            captionText.text = post.caption
            
            // Likes count
            likesCountText.text = "${post.likesCount} likes"
            
            // Like button state
            likeButton.setImageResource(
                if (post.isLikedByCurrentUser) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
        }
        
        // Handle loading state
        if (state.isLoading) {
            // Show loading indicator
        }
        
        // Handle errors
        state.error?.let { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Adapter for displaying comments.
 */
class CommentsAdapter(
    private val onProfileClick: (String) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {
    
    private var comments: List<Comment> = emptyList()
    
    fun submitList(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }
    
    override fun getItemCount() = comments.size
    
    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.comment_profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.comment_username)
        private val contentText: TextView = itemView.findViewById(R.id.comment_content)
        private val timestampText: TextView = itemView.findViewById(R.id.comment_timestamp)
        
        fun bind(comment: Comment) {
            usernameText.text = comment.username
            contentText.text = comment.content
            timestampText.text = getRelativeTime(comment.timestamp)
            
            // Profile image
            comment.userProfileImage?.let { profilePic ->
                decodeBase64Image(profilePic)?.let { bitmap ->
                    profileImage.setImageBitmap(bitmap)
                }
            }
            
            // Click listeners
            profileImage.setOnClickListener { onProfileClick(comment.userId) }
            usernameText.setOnClickListener { onProfileClick(comment.userId) }
        }
        
        private fun decodeBase64Image(base64String: String): Bitmap? {
            return try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }
        
        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            return when {
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "now"
            }
        }
    }
}
