package com.junaidjamshid.i211203.presentation.profile.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

/**
 * Clean Architecture Post Grid Adapter for profile with video support.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PostGridAdapterNew(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, PostGridAdapterNew.PostViewHolder>(PostDiffCallback()) {

    companion object {
        private const val TAG = "PostGridAdapterNew"
    }

    /** Currently playing video post ID */
    private var currentlyPlayingPostId: String? = null

    /** Map of postId to ExoPlayer for video posts */
    private val videoPlayers = mutableMapOf<String, ExoPlayer>()

    /** Global mute state */
    private var isMuted = true
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    /**
     * Play video for a specific post
     */
    fun playVideo(postId: String) {
        if (currentlyPlayingPostId == postId) return

        // Pause current video if any
        currentlyPlayingPostId?.let { pauseVideo(it) }

        currentlyPlayingPostId = postId
        videoPlayers[postId]?.let { player ->
            player.playWhenReady = true
            Log.d(TAG, "Playing video for post: $postId")
        }
    }

    /**
     * Pause video for a specific post
     */
    fun pauseVideo(postId: String) {
        videoPlayers[postId]?.let { player ->
            player.playWhenReady = false
            Log.d(TAG, "Paused video for post: $postId")
        }
        if (currentlyPlayingPostId == postId) {
            currentlyPlayingPostId = null
        }
    }

    /**
     * Pause all videos
     */
    fun pauseAllVideos() {
        videoPlayers.values.forEach { it.playWhenReady = false }
        currentlyPlayingPostId = null
    }

    /**
     * Release all video players - call when fragment is destroyed
     */
    fun releaseAllPlayers() {
        videoPlayers.values.forEach {
            try {
                it.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing player: ${e.message}")
            }
        }
        videoPlayers.clear()
        currentlyPlayingPostId = null
    }
    
    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        private val videoPlayer: PlayerView = itemView.findViewById(R.id.video_player)
        private val videoIcon: ImageView = itemView.findViewById(R.id.video_icon)
        private val videoDuration: TextView = itemView.findViewById(R.id.video_duration)
        private val carouselIcon: ImageView = itemView.findViewById(R.id.carousel_icon)

        private var currentPostId: String? = null

        fun bind(post: Post) {
            currentPostId = post.postId

            if (post.isVideo && post.videoUrl.isNotBlank()) {
                // Video post
                setupVideoPost(post)
            } else {
                // Image post
                setupImagePost(post)
            }

            itemView.setOnClickListener { 
                if (post.isVideo) {
                    // Toggle video playback on click
                    val player = videoPlayers[post.postId]
                    if (player?.isPlaying == true) {
                        pauseVideo(post.postId)
                        videoIcon.visibility = View.VISIBLE
                    } else {
                        playVideo(post.postId)
                        videoIcon.visibility = View.GONE
                    }
                }
                onPostClick(post)
            }
        }

        private fun setupImagePost(post: Post) {
            // Hide video views
            videoThumbnail.visibility = View.GONE
            videoPlayer.visibility = View.GONE
            videoIcon.visibility = View.GONE
            videoDuration.visibility = View.GONE
            videoPlayer.player = null

            // Show image
            postImage.visibility = View.VISIBLE
            
            // Show carousel icon if multiple images
            carouselIcon.visibility = if (post.isCarousel) View.VISIBLE else View.GONE

            // Load image
            val firstImage = post.allImages.firstOrNull() ?: post.postImageUrl
            if (firstImage.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(firstImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        postImage.setImageBitmap(bitmap)
                    } else {
                        postImage.setBackgroundColor(0xFFEFEFEF.toInt())
                    }
                } catch (e: Exception) {
                    postImage.setBackgroundColor(0xFFEFEFEF.toInt())
                }
            }
        }

        private fun setupVideoPost(post: Post) {
            // Hide image views
            postImage.visibility = View.GONE
            carouselIcon.visibility = View.GONE

            // Show video views
            videoThumbnail.visibility = View.VISIBLE
            videoPlayer.visibility = View.VISIBLE
            videoIcon.visibility = View.VISIBLE

            // Show duration if available
            if (post.videoDuration > 0) {
                videoDuration.text = post.formattedDuration
                videoDuration.visibility = View.VISIBLE
            } else {
                videoDuration.visibility = View.GONE
            }

            // Load thumbnail
            if (post.thumbnailUrl.isNotBlank()) {
                Glide.with(itemView.context)
                    .load(post.thumbnailUrl)
                    .centerCrop()
                    .into(videoThumbnail)
            } else {
                videoThumbnail.setBackgroundColor(0xFFEFEFEF.toInt())
            }

            // Setup ExoPlayer
            setupExoPlayer(post)
        }

        private fun setupExoPlayer(post: Post) {
            val player = videoPlayers.getOrPut(post.postId) {
                ExoPlayer.Builder(itemView.context).build().also { newPlayer ->
                    newPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    newPlayer.volume = if (isMuted) 0f else 1f

                    newPlayer.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    if (newPlayer.isPlaying) {
                                        videoThumbnail.visibility = View.GONE
                                        videoIcon.visibility = View.GONE
                                    }
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                videoThumbnail.visibility = View.GONE
                                videoIcon.visibility = View.GONE
                            } else {
                                videoIcon.visibility = View.VISIBLE
                            }
                        }
                    })
                }
            }

            // Set media item if not already set
            val mediaItem = MediaItem.fromUri(post.videoUrl)
            if (player.currentMediaItem?.localConfiguration?.uri?.toString() != post.videoUrl) {
                player.setMediaItem(mediaItem)
                player.prepare()
            }

            // Attach to PlayerView
            videoPlayer.player = player
        }

        fun cleanup() {
            currentPostId?.let { postId ->
                pauseVideo(postId)
            }
            videoPlayer.player = null
        }
    }
    
    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }
        
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
