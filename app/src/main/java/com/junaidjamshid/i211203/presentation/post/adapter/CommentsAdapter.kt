package com.junaidjamshid.i211203.presentation.post.adapter

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
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
import com.junaidjamshid.i211203.domain.model.Comment
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Instagram-style comments adapter.
 */
class CommentsAdapter(
    private val onUserClick: (String) -> Unit,
    private val onLikeClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.comment_profile_image)
        private val commentText: TextView = itemView.findViewById(R.id.comment_text)
        private val timestamp: TextView = itemView.findViewById(R.id.comment_timestamp)
        private val likesCount: TextView = itemView.findViewById(R.id.comment_likes)
        private val replyButton: TextView = itemView.findViewById(R.id.btn_reply)
        private val likeButton: ImageView = itemView.findViewById(R.id.btn_like)

        fun bind(comment: Comment) {
            // Check if this is the post caption (special comment)
            val isCaption = comment.commentId.startsWith("caption_")

            // Build Instagram-style comment text: "username comment text"
            val ssb = SpannableStringBuilder()
            ssb.append(comment.username, StyleSpan(Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.append(" ")
            ssb.append(comment.content)
            commentText.text = ssb

            // Timestamp
            timestamp.text = getTimeAgo(comment.timestamp)

            // For caption, hide likes and reply/like buttons
            if (isCaption) {
                likesCount.visibility = View.GONE
                replyButton.visibility = View.GONE
                likeButton.visibility = View.GONE
            } else {
                replyButton.visibility = View.VISIBLE
                likeButton.visibility = View.VISIBLE
                
                // Likes
                if (comment.likesCount > 0) {
                    likesCount.visibility = View.VISIBLE
                    likesCount.text = "${comment.likesCount} ${if (comment.likesCount == 1) "like" else "likes"}"
                } else {
                    likesCount.visibility = View.GONE
                }

                // Like button state
                if (comment.isLikedByCurrentUser) {
                    likeButton.setImageResource(R.drawable.ic_heart_filled)
                    likeButton.setColorFilter(0xFFED4956.toInt())
                } else {
                    likeButton.setImageResource(R.drawable.ic_heart_outline)
                    likeButton.setColorFilter(0xFF8E8E8E.toInt())
                }
            }

            // Load profile image
            if (comment.userProfileImage.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(comment.userProfileImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile)
            }

            // Click listeners
            profileImage.setOnClickListener { onUserClick(comment.userId) }
            commentText.setOnClickListener { onUserClick(comment.userId) }
            likeButton.setOnClickListener { onLikeClick(comment) }
            replyButton.setOnClickListener { onReplyClick(comment) }
        }

        private fun getTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7

            return when {
                weeks > 0 -> "${weeks}w"
                days > 0 -> "${days}d"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "now"
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}
