package com.junaidjamshid.i211203.presentation.messages.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.messages.DmsFriendItem
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for Instagram-style DMs friends list.
 */
class DmsFriendAdapter(
    private val onFriendClick: (DmsFriendItem) -> Unit
) : ListAdapter<DmsFriendItem, DmsFriendAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dms_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        private val onlineIndicator: View = itemView.findViewById(R.id.online_indicator)
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tv_last_message)

        fun bind(friend: DmsFriendItem) {
            val user = friend.user

            // Username
            tvUsername.text = user.username.ifEmpty { user.fullName }

            // Last message / Full name
            val displayText = if (friend.lastMessage.isNotEmpty()) {
                friend.lastMessage
            } else {
                user.fullName.ifEmpty { "Tap to message" }
            }
            tvLastMessage.text = displayText

            // Online indicator
            onlineIndicator.visibility = if (friend.isOnline) View.VISIBLE else View.GONE

            // Text styling based on unread state
            if (friend.unreadCount > 0) {
                tvUsername.setTextColor(0xFF262626.toInt())
                tvLastMessage.setTextColor(0xFF262626.toInt())
            } else {
                tvUsername.setTextColor(0xFF262626.toInt())
                tvLastMessage.setTextColor(0xFF8E8E8E.toInt())
            }

            // Profile image
            loadProfileImage(user.profilePicture)

            // Click listener
            itemView.setOnClickListener { onFriendClick(friend) }
        }

        private fun loadProfileImage(profilePicture: String?) {
            if (!profilePicture.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(profilePicture, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile)
            }
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<DmsFriendItem>() {
        override fun areItemsTheSame(oldItem: DmsFriendItem, newItem: DmsFriendItem): Boolean {
            return oldItem.user.userId == newItem.user.userId
        }

        override fun areContentsTheSame(oldItem: DmsFriendItem, newItem: DmsFriendItem): Boolean {
            return oldItem == newItem
        }
    }
}
