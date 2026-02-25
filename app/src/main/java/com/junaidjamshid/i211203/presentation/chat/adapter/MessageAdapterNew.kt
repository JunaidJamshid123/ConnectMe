package com.junaidjamshid.i211203.presentation.chat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for chat messages.
 */
class MessageAdapterNew(
    private val currentUserId: String,
    private val onMessageLongClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.sent_message_layout, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.received_message_layout, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }
    
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.txtSentMessage)
        private val timeText: TextView = itemView.findViewById(R.id.txtSentMessageTime)
        
        fun bind(message: Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }
    
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.txtReceivedMessage)
        private val timeText: TextView = itemView.findViewById(R.id.txtReceivedMessageTime)
        
        fun bind(message: Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.messageId == newItem.messageId
        }
        
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
