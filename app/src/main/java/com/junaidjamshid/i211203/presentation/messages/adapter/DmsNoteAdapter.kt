package com.junaidjamshid.i211203.presentation.messages.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.presentation.messages.DmsNoteUser
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for Instagram-style Notes/Stories row in DMs.
 */
class DmsNoteAdapter(
    private val onNoteClick: (DmsNoteUser) -> Unit
) : ListAdapter<DmsNoteUser, DmsNoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dms_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        private val onlineIndicator: View = itemView.findViewById(R.id.online_indicator)
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val addNoteContainer: FrameLayout = itemView.findViewById(R.id.add_note_container)
        private val storyRing: CircleImageView = itemView.findViewById(R.id.story_ring)

        fun bind(noteUser: DmsNoteUser) {
            // Username
            tvUsername.text = noteUser.username

            // Profile image
            loadProfileImage(noteUser.profilePicture)

            if (noteUser.isCurrentUser) {
                // Current user - show add note button
                addNoteContainer.visibility = View.VISIBLE
                onlineIndicator.visibility = View.GONE
                storyRing.visibility = View.GONE
            } else {
                // Other users
                addNoteContainer.visibility = View.GONE
                onlineIndicator.visibility = if (noteUser.isOnline) View.VISIBLE else View.GONE
                storyRing.visibility = View.GONE // Can show for users with stories
            }

            // Click listener
            itemView.setOnClickListener { onNoteClick(noteUser) }
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

    class NoteDiffCallback : DiffUtil.ItemCallback<DmsNoteUser>() {
        override fun areItemsTheSame(oldItem: DmsNoteUser, newItem: DmsNoteUser): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: DmsNoteUser, newItem: DmsNoteUser): Boolean {
            return oldItem == newItem
        }
    }
}
