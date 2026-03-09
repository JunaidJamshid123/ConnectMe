package com.junaidjamshid.i211203.presentation.discover

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
import com.junaidjamshid.i211203.domain.model.User

/**
 * Adapter for the vertical list of suggested users in the Discover People page.
 * Shows profile picture, username, full name/reason, follow button, and dismiss.
 */
class SuggestionListAdapter(
    private val onFollowClick: (String) -> Unit,
    private val onDismissClick: (String) -> Unit,
    private val onProfileClick: (String) -> Unit
) : ListAdapter<User, SuggestionListAdapter.ListViewHolder>(UserDiffCallback()) {

    /** Set of user IDs the current user is following */
    var followingUserIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion_list, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.suggestion_profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.suggestion_username)
        private val fullNameText: TextView = itemView.findViewById(R.id.suggestion_full_name)
        private val followButton: TextView = itemView.findViewById(R.id.btn_follow)
        private val dismissButton: ImageView = itemView.findViewById(R.id.btn_dismiss)

        fun bind(user: User) {
            usernameText.text = user.username
            fullNameText.text = user.fullName.ifEmpty { "Suggested for you" }

            // Profile image
            loadBase64Image(user.profilePicture ?: "", profileImage)

            // Follow state
            val isFollowing = followingUserIds.contains(user.userId)
            updateFollowButton(isFollowing)

            followButton.setOnClickListener {
                onFollowClick(user.userId)
            }

            dismissButton.setOnClickListener {
                onDismissClick(user.userId)
            }

            itemView.setOnClickListener {
                onProfileClick(user.userId)
            }
        }

        private fun updateFollowButton(isFollowing: Boolean) {
            if (isFollowing) {
                followButton.text = "Following"
                followButton.setBackgroundResource(R.drawable.bg_following_button)
                followButton.setTextColor(0xFF262626.toInt())
            } else {
                followButton.text = "Follow"
                followButton.setBackgroundResource(R.drawable.bg_follow_button)
                followButton.setTextColor(0xFFFFFFFF.toInt())
            }
        }

        private fun loadBase64Image(base64: String, imageView: ImageView) {
            if (base64.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        return
                    }
                } catch (_: Exception) { }
            }
            imageView.setImageResource(R.drawable.default_profile)
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) =
            oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: User, newItem: User) =
            oldItem == newItem
    }
}
