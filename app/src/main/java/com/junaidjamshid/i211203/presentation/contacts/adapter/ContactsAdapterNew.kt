package com.junaidjamshid.i211203.presentation.contacts.adapter

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
import com.junaidjamshid.i211203.presentation.contacts.ContactItem
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for contacts list.
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
        
        fun bind(contact: ContactItem) {
            usernameText.text = contact.user.username
            lastMessageText.text = contact.lastMessage
            
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
            
            itemView.setOnClickListener { onContactClick(contact) }
        }
        
        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
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
