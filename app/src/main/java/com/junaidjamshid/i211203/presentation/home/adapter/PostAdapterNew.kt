package com.junaidjamshid.i211203.presentation.home.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post

/**
 * Clean Architecture Post Adapter using ListAdapter with DiffUtil.
 */
class PostAdapterNew(
    private val onLikeClick: (String) -> Unit,
    private val onCommentClick: (String) -> Unit,
    private val onShareClick: (String) -> Unit,
    private val onSaveClick: (String) -> Unit,
    private val onProfileClick: (String) -> Unit,
    private val onMenuClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapterNew.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.username_text)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val heartButton: ImageView = itemView.findViewById(R.id.heart)
        private val commentButton: ImageView = itemView.findViewById(R.id.comment)
        private val shareButton: ImageView = itemView.findViewById(R.id.send)
        private val saveButton: ImageView = itemView.findViewById(R.id.save)
        private val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        private val postCaption: TextView = itemView.findViewById(R.id.post_caption)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val menuButton: ImageView = itemView.findViewById(R.id.menu_dots)

        fun bind(post: Post) {
            usernameText.text = post.username
            likesCount.text = "${post.likesCount} likes"
            postCaption.text = "${post.username} ${post.caption}"
            timestamp.text = getTimeAgo(post.timestamp)
            
            // Load profile image
            if (post.userProfileImage.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(post.userProfileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.junaid1)
                }
            }
            
            // Load post image
            if (post.postImageUrl.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(post.postImageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    postImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // Handle error
                }
            }
            
            // Update like button state
            heartButton.setImageResource(
                if (post.isLikedByCurrentUser) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
            
            // Click listeners
            heartButton.setOnClickListener { onLikeClick(post.postId) }
            commentButton.setOnClickListener { onCommentClick(post.postId) }
            shareButton.setOnClickListener { onShareClick(post.postId) }
            saveButton.setOnClickListener { onSaveClick(post.postId) }
            profileImage.setOnClickListener { onProfileClick(post.userId) }
            usernameText.setOnClickListener { onProfileClick(post.userId) }
            menuButton.setOnClickListener { onMenuClick(post) }
        }
        
        private fun getTimeAgo(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            return when {
                days > 0 -> "$days days ago"
                hours > 0 -> "$hours hours ago"
                minutes > 0 -> "$minutes minutes ago"
                else -> "Just now"
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
