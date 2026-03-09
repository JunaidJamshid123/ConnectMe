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
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Data class representing a note/story item in the horizontal row.
 */
data class NoteItem(
    val userId: String = "",
    val username: String = "",
    val profilePicture: String? = null,
    val noteText: String? = null,
    val isOnline: Boolean = false,
    val isCurrentUser: Boolean = false
)

/**
 * Adapter for the horizontal Notes / Stories row at the top of the DM screen
 * (Instagram-style).
 */
class NotesAdapter(
    private val onNoteClick: (NoteItem) -> Unit = {}
) : ListAdapter<NoteItem, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note_story, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.imgNoteProfile)
        private val usernameText: TextView = itemView.findViewById(R.id.tvNoteUsername)
        private val noteBubble: TextView = itemView.findViewById(R.id.tvNoteBubble)
        private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)

        fun bind(item: NoteItem) {
            // Username
            usernameText.text = if (item.isCurrentUser) "Your note" else item.username

            // Profile picture
            if (!item.profilePicture.isNullOrEmpty()) {
                try {
                    val decodedBytes = Base64.decode(item.profilePicture, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile)
            }

            // Note bubble
            if (!item.noteText.isNullOrEmpty()) {
                noteBubble.visibility = View.VISIBLE
                noteBubble.text = item.noteText
            } else {
                noteBubble.visibility = View.GONE
            }

            // Online indicator
            onlineIndicator.visibility = if (item.isOnline) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onNoteClick(item) }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<NoteItem>() {
        override fun areItemsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: NoteItem, newItem: NoteItem): Boolean {
            return oldItem == newItem
        }
    }
}
