package com.junaidjamshid.i211203.presentation.chat.adapter

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.card.MaterialCardView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Message
import com.junaidjamshid.i211203.domain.model.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for chat messages with smooth animations.
 * Supports text, image, and voice messages.
 */
class MessageAdapterNew(
    private val currentUserId: String,
    private val onMessageLongClick: (Message) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
    
    private var lastAnimatedPosition = -1
    private var currentlyPlayingUrl: String? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    
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
        
        // Animate new messages
        if (position > lastAnimatedPosition) {
            val animRes = if (holder is SentMessageViewHolder) {
                R.anim.message_slide_in_right
            } else {
                R.anim.message_slide_in_left
            }
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, animRes)
            holder.itemView.startAnimation(animation)
            lastAnimatedPosition = position
        }
    }
    
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }
    
    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.txtSentMessage)
        private val timeText: TextView = itemView.findViewById(R.id.txtSentMessageTime)
        private val statusText: TextView = itemView.findViewById(R.id.txtSentMessageStatus)
        private val cardImage: MaterialCardView = itemView.findViewById(R.id.cardSentImage)
        private val imageView: ImageView = itemView.findViewById(R.id.imgSentMessage)
        private val layoutVoice: LinearLayout = itemView.findViewById(R.id.layoutSentVoice)
        private val btnPlay: ImageView = itemView.findViewById(R.id.btnPlaySentVoice)
        private val seekBar: SeekBar = itemView.findViewById(R.id.seekBarSentVoice)
        private val durationText: TextView = itemView.findViewById(R.id.txtSentVoiceDuration)
        
        fun bind(message: Message) {
            timeText.text = formatTime(message.timestamp)
            
            // Show read status
            statusText.visibility = View.VISIBLE
            statusText.text = if (message.isRead) "Seen" else "Sent"
            statusText.setTextColor(
                if (message.isRead) 0xFF0095F6.toInt() else 0xFF8E8E8E.toInt()
            )
            
            // Handle different message types
            when (message.messageType) {
                MessageType.IMAGE -> {
                    messageText.visibility = View.GONE
                    cardImage.visibility = View.VISIBLE
                    layoutVoice.visibility = View.GONE
                    
                    message.imageUrl?.let { url ->
                        Glide.with(itemView.context)
                            .load(url)
                            .transform(RoundedCorners(32))
                            .placeholder(R.drawable.placeholder_image)
                            .into(imageView)
                    }
                }
                MessageType.VOICE -> {
                    messageText.visibility = View.GONE
                    cardImage.visibility = View.GONE
                    layoutVoice.visibility = View.VISIBLE
                    
                    // Extract duration from content if available
                    val durationMatch = Regex("\\((\\d+:\\d+)\\)").find(message.content)
                    durationText.text = durationMatch?.groupValues?.get(1) ?: "0:00"
                    
                    setupVoicePlayer(message.imageUrl, btnPlay, seekBar, durationText)
                }
                else -> {
                    messageText.visibility = View.VISIBLE
                    cardImage.visibility = View.GONE
                    layoutVoice.visibility = View.GONE
                    messageText.text = message.content
                }
            }
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }
    
    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.txtReceivedMessage)
        private val timeText: TextView = itemView.findViewById(R.id.txtReceivedMessageTime)
        private val cardImage: MaterialCardView = itemView.findViewById(R.id.cardReceivedImage)
        private val imageView: ImageView = itemView.findViewById(R.id.imgReceivedMessage)
        private val layoutVoice: LinearLayout = itemView.findViewById(R.id.layoutReceivedVoice)
        private val btnPlay: ImageView = itemView.findViewById(R.id.btnPlayReceivedVoice)
        private val seekBar: SeekBar = itemView.findViewById(R.id.seekBarReceivedVoice)
        private val durationText: TextView = itemView.findViewById(R.id.txtReceivedVoiceDuration)
        
        fun bind(message: Message) {
            timeText.text = formatTime(message.timestamp)
            
            // Handle different message types
            when (message.messageType) {
                MessageType.IMAGE -> {
                    messageText.visibility = View.GONE
                    cardImage.visibility = View.VISIBLE
                    layoutVoice.visibility = View.GONE
                    
                    message.imageUrl?.let { url ->
                        Glide.with(itemView.context)
                            .load(url)
                            .transform(RoundedCorners(32))
                            .placeholder(R.drawable.placeholder_image)
                            .into(imageView)
                    }
                }
                MessageType.VOICE -> {
                    messageText.visibility = View.GONE
                    cardImage.visibility = View.GONE
                    layoutVoice.visibility = View.VISIBLE
                    
                    // Extract duration from content if available
                    val durationMatch = Regex("\\((\\d+:\\d+)\\)").find(message.content)
                    durationText.text = durationMatch?.groupValues?.get(1) ?: "0:00"
                    
                    setupVoicePlayer(message.imageUrl, btnPlay, seekBar, durationText)
                }
                else -> {
                    messageText.visibility = View.VISIBLE
                    cardImage.visibility = View.GONE
                    layoutVoice.visibility = View.GONE
                    messageText.text = message.content
                }
            }
        }
    }
    
    private fun setupVoicePlayer(
        url: String?,
        btnPlay: ImageView,
        seekBar: SeekBar,
        durationText: TextView
    ) {
        if (url.isNullOrEmpty()) {
            android.util.Log.e("VoicePlayer", "Voice URL is null or empty")
            btnPlay.setOnClickListener {
                android.widget.Toast.makeText(
                    btnPlay.context,
                    "Voice URL is missing!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        
        android.util.Log.d("VoicePlayer", "Setting up player for URL: $url")
        
        var isPlaying = currentlyPlayingUrl == url && mediaPlayer?.isPlaying == true
        
        btnPlay.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        
        btnPlay.setOnClickListener {
            android.util.Log.d("VoicePlayer", "Play clicked, URL: $url")
            
            if (currentlyPlayingUrl == url && mediaPlayer != null) {
                // Toggle play/pause
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    btnPlay.setImageResource(R.drawable.ic_play)
                } else {
                    mediaPlayer?.start()
                    btnPlay.setImageResource(R.drawable.ic_pause)
                    updateSeekBar(seekBar, durationText)
                }
            } else {
                // Start new playback
                stopCurrentPlayback()
                currentlyPlayingUrl = url
                
                try {
                    mediaPlayer = MediaPlayer().apply {
                        // Set audio attributes for streaming
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(url)
                        setOnPreparedListener {
                            android.util.Log.d("VoicePlayer", "MediaPlayer prepared, starting playback")
                            start()
                            btnPlay.setImageResource(R.drawable.ic_pause)
                            updateSeekBar(seekBar, durationText)
                        }
                        setOnCompletionListener {
                            android.util.Log.d("VoicePlayer", "Playback completed")
                            btnPlay.setImageResource(R.drawable.ic_play)
                            seekBar.progress = 0
                            currentlyPlayingUrl = null
                        }
                        setOnErrorListener { _, what, extra ->
                            android.util.Log.e("VoicePlayer", "MediaPlayer error: what=$what, extra=$extra, URL=$url")
                            btnPlay.setImageResource(R.drawable.ic_play)
                            currentlyPlayingUrl = null
                            android.widget.Toast.makeText(
                                btnPlay.context,
                                "Could not play audio (error: $what)",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            true
                        }
                        prepareAsync()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VoicePlayer", "Error creating MediaPlayer: ${e.message}", e)
                    android.widget.Toast.makeText(
                        btnPlay.context,
                        "Error: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && currentlyPlayingUrl == url) {
                    mediaPlayer?.let {
                        val seekPos = (progress / 100f * it.duration).toInt()
                        it.seekTo(seekPos)
                    }
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
    
    private fun updateSeekBar(seekBar: SeekBar, durationText: TextView) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = (player.currentPosition.toFloat() / player.duration * 100).toInt()
                        seekBar.progress = progress
                        
                        val current = player.currentPosition / 1000
                        val minutes = current / 60
                        val seconds = current % 60
                        durationText.text = String.format("%d:%02d", minutes, seconds)
                        
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }, 100)
    }
    
    private fun stopCurrentPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentlyPlayingUrl = null
    }
    
    fun releasePlayer() {
        stopCurrentPlayback()
        handler.removeCallbacksAndMessages(null)
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
