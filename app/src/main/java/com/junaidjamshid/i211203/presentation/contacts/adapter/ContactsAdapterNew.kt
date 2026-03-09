package com.junaidjamshid.i211203.presentation.contacts.adapter

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
import com.junaidjamshid.i211203.presentation.contacts.ContactItem
import de.hdodenhof.circleimageview.CircleImageView
import java.util.concurrent.TimeUnit

/**
 * Adapter for the Instagram-style DM chat list.
 */
class ContactsAdapterNew(
    private val onContactClick: (ContactItem) -> Unit
) : ListAdapter<ContactItem, ContactsAdapterNew.ContactViewHolder>(ContactDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.contact_image)
        private val usernameText: TextView = itemView.findViewById(R.id.contact_name)
        private val lastMessageText: TextView = itemView.findViewById(R.id.contact_last_message)
        private val timeText: TextView = itemView.findViewById(R.id.contact_time)
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)
        private val cameraIcon: ImageView = itemView.findViewById(R.id.cameraIcon)
        private val foregroundLayout: View = itemView.findViewById(R.id.foregroundLayout)
        
        fun bind(contact: ContactItem) {
            usernameText.text = contact.user.username

            // Last message text
            if (contact.lastMessage.isNotEmpty()) {
                lastMessageText.text = contact.lastMessage
            } else {
                lastMessageText.text = ""
            }

            // Time display
            if (contact.lastMessageTime > 0) {
                timeText.text = " · ${formatRelativeTime(contact.lastMessageTime)}"
                timeText.visibility = View.VISIBLE
            } else {
                timeText.visibility = View.GONE
            }

            // Online indicator
            onlineIndicator.visibility = if (contact.isOnline) View.VISIBLE else View.GONE

            // Unread state: bold name + darker message color
            if (contact.unreadCount > 0) {
                usernameText.setTextColor(0xFF262626.toInt())
                lastMessageText.setTextColor(0xFF262626.toInt())
                timeText.setTextColor(0xFF262626.toInt())
            } else {
                usernameText.setTextColor(0xFF262626.toInt())
                lastMessageText.setTextColor(0xFF8E8E8E.toInt())
                timeText.setTextColor(0xFF8E8E8E.toInt())
            }
            
            // Load profile image
            if (!contact.user.profilePicture.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(contact.user.profilePicture, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile)
            }
            
            foregroundLayout.setOnClickListener { onContactClick(contact) }
        }

        /**
         * Format timestamp to Instagram-style relative time (e.g., "now", "3h", "2d", "1w")
         */
        private fun formatRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val weeks = days / 7

            return when {
                minutes < 1 -> "now"
                minutes < 60 -> "${minutes}m"
                hours < 24 -> "${hours}h"
                days < 7 -> "${days}d"
                weeks < 52 -> "${weeks}w"
                else -> "${days / 365}y"
            }
        }
    }
    
    class ContactDiffCallback : DiffUtil.ItemCallback<ContactItem>() {
        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            return oldItem.user.userId == newItem.user.userId
        }
        
        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            return oldItem == newItem
        }
    }
}
