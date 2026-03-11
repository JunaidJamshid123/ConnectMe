package com.junaidjamshid.i211203.presentation.reels.adapter

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post
import de.hdodenhof.circleimageview.CircleImageView

/**
 * Adapter for Instagram-style Reels - full screen vertical video feed.
 * Uses ViewPager2 for vertical swiping between reels.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ReelsAdapter(
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onSaveClick: (Post) -> Unit,
    private val onProfileClick: (Post) -> Unit,
    private val onFollowClick: (Post) -> Unit,
    private val onMoreClick: (Post) -> Unit
) : ListAdapter<Post, ReelsAdapter.ReelViewHolder>(ReelDiffCallback()) {

    companion object {
        private const val TAG = "ReelsAdapter"
    }

    /** Map of position to ExoPlayer for video playback */
    private val players = mutableMapOf<Int, ExoPlayer>()

    /** Currently playing position */
    private var currentPlayingPosition = -1

    /** Global mute state */
    private var isMuted = false

    /** Handler for progress updates */
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reel, parent, false)
        // Ensure full screen height for ViewPager2 items
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return ReelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun onViewRecycled(holder: ReelViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    /**
     * Play video at specific position.
     */
    fun playAt(position: Int) {
        if (position < 0 || position >= itemCount) return
        
        // Pause current video if different position
        if (currentPlayingPosition != position && currentPlayingPosition >= 0) {
            pauseAt(currentPlayingPosition)
        }

        currentPlayingPosition = position
        players[position]?.let { player ->
            player.playWhenReady = true
            Log.d(TAG, "Playing reel at position: $position")
        }
    }

    /**
     * Pause video at specific position.
     */
    fun pauseAt(position: Int) {
        players[position]?.let { player ->
            player.playWhenReady = false
            Log.d(TAG, "Paused reel at position: $position")
        }
    }

    /**
     * Pause all videos.
     */
    fun pauseAll() {
        players.values.forEach { it.playWhenReady = false }
        currentPlayingPosition = -1
    }

    /**
     * Toggle mute state.
     */
    fun toggleMute(): Boolean {
        isMuted = !isMuted
        players.values.forEach { it.volume = if (isMuted) 0f else 1f }
        return isMuted
    }

    /**
     * Release all players - call when fragment is destroyed.
     */
    fun releaseAll() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        players.values.forEach {
            try {
                it.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing player: ${e.message}")
            }
        }
        players.clear()
        currentPlayingPosition = -1
    }

    inner class ReelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.video_player)
        private val thumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.video_progress)
        private val btnPlayPause: ImageView = itemView.findViewById(R.id.btn_play_pause)
        private val seekBar: ProgressBar = itemView.findViewById(R.id.video_seek_bar)

        private val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val tvCaption: TextView = itemView.findViewById(R.id.tv_caption)
        private val tvMusicName: TextView = itemView.findViewById(R.id.tv_music_name)
        private val musicContainer: View = itemView.findViewById(R.id.music_container)

        private val btnLike: ImageView = itemView.findViewById(R.id.btn_like)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tv_likes_count)
        private val btnComment: ImageView = itemView.findViewById(R.id.btn_comment)
        private val tvCommentsCount: TextView = itemView.findViewById(R.id.tv_comments_count)
        private val btnShare: ImageView = itemView.findViewById(R.id.btn_share)
        private val btnSave: ImageView = itemView.findViewById(R.id.btn_save)
        private val btnMore: ImageView = itemView.findViewById(R.id.btn_more)
        private val btnFollow: ImageView = itemView.findViewById(R.id.btn_follow)
        private val btnFollowText: TextView = itemView.findViewById(R.id.btn_follow_text)

        private var currentPosition = -1
        private var currentPost: Post? = null

        fun bind(post: Post, position: Int) {
            currentPosition = position
            currentPost = post

            // Setup user info
            tvUsername.text = post.username
            tvCaption.text = if (post.caption.isNotEmpty()) post.caption else ""
            tvCaption.visibility = if (post.caption.isNotEmpty()) View.VISIBLE else View.GONE

            // Music info
            if (post.hasMusic) {
                musicContainer.visibility = View.VISIBLE
                tvMusicName.text = "${post.musicName} · ${post.musicArtist}"
                tvMusicName.isSelected = true // Enable marquee
            } else {
                musicContainer.visibility = View.VISIBLE
                tvMusicName.text = "Original audio · ${post.username}"
            }

            // Like count and state
            tvLikesCount.text = formatCount(post.likesCount)
            btnLike.setImageResource(
                if (post.isLikedByCurrentUser) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
            if (post.isLikedByCurrentUser) {
                btnLike.setColorFilter(itemView.context.getColor(android.R.color.holo_red_light))
            } else {
                btnLike.setColorFilter(itemView.context.getColor(android.R.color.white))
            }

            // Comments count
            tvCommentsCount.text = formatCount(post.commentsCount)

            // Save state
            btnSave.setImageResource(
                if (post.isSavedByCurrentUser) R.drawable.ic_bookmark_filled
                else R.drawable.ic_bookmark
            )

            // Profile image
            loadProfileImage(post.userProfileImage)

            // Load thumbnail
            loadThumbnail(post)

            // Setup video player
            setupPlayer(post, position)

            // Click listeners
            setupClickListeners(post)
        }

        private fun loadProfileImage(profilePicture: String?) {
            if (!profilePicture.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(profilePicture, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.default_profile)
                }
            } else {
                profileImage.setImageResource(R.drawable.default_profile)
            }
        }

        private fun loadThumbnail(post: Post) {
            thumbnail.visibility = View.VISIBLE
            
            if (post.thumbnailUrl.isNotBlank()) {
                Glide.with(itemView.context)
                    .load(post.thumbnailUrl)
                    .centerCrop()
                    .into(thumbnail)
            } else if (post.postImageUrl.isNotBlank()) {
                try {
                    val bytes = Base64.decode(post.postImageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    thumbnail.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    thumbnail.setBackgroundColor(0xFF1A1A1A.toInt())
                }
            } else {
                thumbnail.setBackgroundColor(0xFF1A1A1A.toInt())
            }
        }

        private fun setupPlayer(post: Post, position: Int) {
            val player = players.getOrPut(position) {
                ExoPlayer.Builder(itemView.context).build().also { newPlayer ->
                    newPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    newPlayer.volume = if (isMuted) 0f else 1f

                    newPlayer.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    progressBar.visibility = View.VISIBLE
                                }
                                Player.STATE_READY -> {
                                    progressBar.visibility = View.GONE
                                    if (newPlayer.isPlaying) {
                                        thumbnail.visibility = View.GONE
                                        btnPlayPause.visibility = View.GONE
                                        startProgressUpdates(newPlayer)
                                    }
                                }
                                Player.STATE_ENDED,
                                Player.STATE_IDLE -> {
                                    progressBar.visibility = View.GONE
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                thumbnail.visibility = View.GONE
                                btnPlayPause.animate().alpha(0f).setDuration(200).withEndAction {
                                    btnPlayPause.visibility = View.GONE
                                }.start()
                                startProgressUpdates(newPlayer)
                            } else {
                                stopProgressUpdates()
                            }
                        }
                    })
                }
            }

            // Set media item if different
            val mediaItem = MediaItem.fromUri(post.videoUrl)
            if (player.currentMediaItem?.localConfiguration?.uri?.toString() != post.videoUrl) {
                player.setMediaItem(mediaItem)
                player.prepare()
            }

            // Attach to PlayerView
            playerView.player = player

            // Tap to play/pause
            playerView.setOnClickListener {
                if (player.isPlaying) {
                    player.playWhenReady = false
                    btnPlayPause.setImageResource(R.drawable.ic_play_circle)
                    btnPlayPause.alpha = 0.8f
                    btnPlayPause.visibility = View.VISIBLE
                } else {
                    player.playWhenReady = true
                }
            }
        }

        private fun startProgressUpdates(player: ExoPlayer) {
            stopProgressUpdates()
            progressRunnable = object : Runnable {
                override fun run() {
                    if (player.duration > 0) {
                        val progress = ((player.currentPosition.toFloat() / player.duration) * 100).toInt()
                        seekBar.progress = progress
                    }
                    progressHandler.postDelayed(this, 100)
                }
            }
            progressHandler.post(progressRunnable!!)
        }

        private fun stopProgressUpdates() {
            progressRunnable?.let { progressHandler.removeCallbacks(it) }
        }

        private fun setupClickListeners(post: Post) {
            btnLike.setOnClickListener { onLikeClick(post) }
            btnComment.setOnClickListener { onCommentClick(post) }
            btnShare.setOnClickListener { onShareClick(post) }
            btnSave.setOnClickListener { onSaveClick(post) }
            btnMore.setOnClickListener { onMoreClick(post) }
            profileImage.setOnClickListener { onProfileClick(post) }
            tvUsername.setOnClickListener { onProfileClick(post) }
            btnFollow.setOnClickListener { onFollowClick(post) }
            btnFollowText.setOnClickListener { onFollowClick(post) }
        }

        private fun formatCount(count: Int): String {
            return when {
                count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }

        fun cleanup() {
            stopProgressUpdates()
            playerView.player = null
        }
    }

    class ReelDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
