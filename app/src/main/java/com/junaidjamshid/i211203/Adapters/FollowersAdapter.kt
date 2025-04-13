package com.junaidjamshid.i211203.Adapters

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

class FollowersAdapter(
    private var followersList: List<User>,
    private val onFollowerClick: (User) -> Unit,
    private val onMessageClick: (User) -> Unit
) : RecyclerView.Adapter<FollowersAdapter.FollowerViewHolder>() {

    private var filteredList: List<User> = followersList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerViewHolder {
        // Using the same layout as contacts
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return FollowerViewHolder(view)
    }

    override fun onBindViewHolder(holder: FollowerViewHolder, position: Int) {
        val follower = filteredList[position]
        holder.bind(follower)
    }

    override fun getItemCount(): Int = filteredList.size

    fun filterFollowers(query: String) {
        filteredList = if (query.isEmpty()) {
            followersList
        } else {
            followersList.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun updateFollowers(newFollowers: List<User>) {
        followersList = newFollowers
        filteredList = newFollowers
        notifyDataSetChanged()
    }

    inner class FollowerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val followerImage: CircleImageView = itemView.findViewById(R.id.contact_image)
        private val followerName: TextView = itemView.findViewById(R.id.contact_name)
        private val followerLastSeen: TextView = itemView.findViewById(R.id.contact_last_message)
        private val messageButton: ImageView = itemView.findViewById(R.id.contact_message)

        fun bind(user: User) {
            // Set follower name - use full name if available, otherwise username
            followerName.text = if (user.fullName.isNotEmpty()) user.fullName else user.username

            // Set profile image if available
            user.getProfilePictureBytes()?.let { imageBytes ->
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                followerImage.setImageBitmap(bitmap)
            } ?: run {
                followerImage.setImageResource(R.drawable.default_profile)
            }

            // Set online status or last seen info
            if (user.onlineStatus) {
                followerLastSeen.text = "Active now"
            } else {
                followerLastSeen.text = "Active • ${formatLastSeen(user.lastSeen)}"
            }

            // Set click listeners
            itemView.setOnClickListener {
                onFollowerClick(user)
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