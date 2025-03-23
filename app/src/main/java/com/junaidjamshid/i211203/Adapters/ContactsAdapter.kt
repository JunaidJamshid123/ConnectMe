package com.junaidjamshid.i211203.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.models.User
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ContactsAdapter(
    private var contactsList: List<User>,
    private val onContactClick: (User) -> Unit,
    private val onMessageClick: (User) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private var filteredList: List<User> = contactsList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredList[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = filteredList.size

    fun filterContacts(query: String) {
        filteredList = if (query.isEmpty()) {
            contactsList
        } else {
            contactsList.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateContacts(newContacts: List<User>) {
        contactsList = newContacts
        filteredList = newContacts
        notifyDataSetChanged()
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contactImage: CircleImageView = itemView.findViewById(R.id.contact_image)
        private val contactName: TextView = itemView.findViewById(R.id.contact_name)
        private val contactLastSeen: TextView = itemView.findViewById(R.id.contact_last_message)
        private val messageButton: ImageView = itemView.findViewById(R.id.contact_message)

        fun bind(user: User) {
            // Set contact name - use full name if available, otherwise username
            contactName.text = if (user.fullName.isNotEmpty()) user.fullName else user.username

            // Set profile image if available
            user.getProfilePictureBytes()?.let { imageBytes ->
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                contactImage.setImageBitmap(bitmap)
            } ?: run {
                contactImage.setImageResource(R.drawable.default_profile)
            }

            // Set online status or last seen info
            if (user.onlineStatus) {
                contactLastSeen.text = "Active now"
            } else {
                contactLastSeen.text = "Active • ${formatLastSeen(user.lastSeen)}"
            }

            // Set click listeners
            itemView.setOnClickListener {
                onContactClick(user)
            }

            messageButton.setOnClickListener {
                onMessageClick(user)
            }
        }

        private fun formatLastSeen(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}