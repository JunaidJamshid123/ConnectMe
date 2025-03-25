package com.junaidjamshid.i211203

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val context: Context,
    private val messageList: ArrayList<Message>,
    private val receiverProfileImage: CircleImageView? = null,
    private val onMessageEditListener: ((Message) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        private const val MESSAGE_EDIT_WINDOW_MINUTES = 5
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sentMessageText: TextView = itemView.findViewById(R.id.txtSentMessage)
        private val sentMessageTime: TextView = itemView.findViewById(R.id.txtSentMessageTime)
        private val sentMessageStatus: TextView = itemView.findViewById(R.id.txtSentMessageStatus)

        fun bind(message: Message) {
            // Handle deleted messages
            if (message.isDeleted) {
                sentMessageText.text = "This message was deleted"
                sentMessageText.setTextColor(context.resources.getColor(android.R.color.darker_gray))
                sentMessageStatus.visibility = View.GONE
                itemView.setOnClickListener(null)
                return
            }

            // Handle edited messages
            val displayMessage = if (message.isEdited) "${message.message} (edited)" else message.message
            sentMessageText.text = displayMessage
            sentMessageTime.text = formatTimestamp(message.timestamp)

            // Message status
            sentMessageStatus.text = when {
                message.isRead -> "Read"
                message.isVanishMode -> "Vanish Mode"
                else -> "Sent"
            }

            // Long click to edit or delete
            itemView.setOnLongClickListener {
                val timeSinceMessage = System.currentTimeMillis() - message.timestamp
                if (timeSinceMessage <= MESSAGE_EDIT_WINDOW_MINUTES * 60 * 1000) {
                    showMessageOptionsDialog(message)
                } else {
                    Toast.makeText(context, "Cannot edit message after 5 minutes", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val receivedMessageText: TextView = itemView.findViewById(R.id.txtReceivedMessage)
        private val receivedMessageTime: TextView = itemView.findViewById(R.id.txtReceivedMessageTime)
        private val receiverProfileImageView: CircleImageView = itemView.findViewById(R.id.imgReceiverProfile)

        fun bind(message: Message) {
            // Handle deleted messages
            if (message.isDeleted) {
                receivedMessageText.text = "This message was deleted"
                receivedMessageText.setTextColor(context.resources.getColor(android.R.color.darker_gray))
                return
            }

            // Handle edited messages
            val displayMessage = if (message.isEdited) "${message.message} (edited)" else message.message
            receivedMessageText.text = displayMessage
            receivedMessageTime.text = formatTimestamp(message.timestamp)

            // Set receiver profile image if available
            receiverProfileImage?.let {
                receiverProfileImageView.setImageDrawable(it.drawable)
            }

            // Mark message as read for vanish mode
            if (message.isVanishMode && !message.isRead) {
                markMessageAsRead(message)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.sent_message_layout, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.received_message_layout, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(currentMessage)
            is ReceivedMessageViewHolder -> holder.bind(currentMessage)
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        return if (currentMessage.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    private fun showMessageOptionsDialog(message: Message) {
        val options = arrayOf("Edit Message", "Delete Message")
        AlertDialog.Builder(context)
            .setTitle("Message Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onMessageEditListener?.invoke(message)
                    1 -> deleteMessage(message)
                }
            }
            .show()
    }

    private fun deleteMessage(message: Message) {
        val database = FirebaseDatabase.getInstance()
        val messageRef = database.reference.child("Chats").child(message.messageId)

        // Soft delete - update message instead of removing
        val updateMap = mapOf(
            "deleted" to true,
            "message" to "Message deleted"
        )

        messageRef.updateChildren(updateMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markMessageAsRead(message: Message) {
        val database = FirebaseDatabase.getInstance()
        val messageRef = database.reference.child("Chats").child(message.messageId)

        messageRef.child("read").setValue(true)
            .addOnSuccessListener {
                // Optional: Add any additional logic for read messages
            }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}