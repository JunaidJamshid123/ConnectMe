package com.junaidjamshid.i211203

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.junaidjamshid.i211203.Models.Message
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val messageList: ArrayList<Message>,
    private val receiverProfileImage: CircleImageView? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sentMessageText: TextView = itemView.findViewById(R.id.txtSentMessage)
        private val sentMessageTime: TextView = itemView.findViewById(R.id.txtSentMessageTime)

        fun bind(message: Message) {
            sentMessageText.text = message.message
            sentMessageTime.text = formatTimestamp(message.timestamp)
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val receivedMessageText: TextView = itemView.findViewById(R.id.txtReceivedMessage)
        private val receivedMessageTime: TextView = itemView.findViewById(R.id.txtReceivedMessageTime)
        private val receiverProfileImageView: CircleImageView = itemView.findViewById(R.id.imgReceiverProfile)

        fun bind(message: Message) {
            receivedMessageText.text = message.message
            receivedMessageTime.text = formatTimestamp(message.timestamp)

            // Set receiver profile image if available
            receiverProfileImage?.let {
                receiverProfileImageView.setImageDrawable(it.drawable)
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

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}