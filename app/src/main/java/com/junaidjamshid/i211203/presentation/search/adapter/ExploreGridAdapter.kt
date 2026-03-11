package com.junaidjamshid.i211203.presentation.search.adapter

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
 * Adapter for Instagram-style explore grid with mixed tile sizes and video support.
 * 
 * Instagram's explore grid pattern repeats every 10 items in two groups:
 * 
 * Group A (indices 0-4):
 *   [S][S][L]   <- L is large (2 cols wide, 2 rows tall) at position 2
 *   [S][S][ ]   <- L continues here
 * 
 * Group B (indices 5-9):
 *   [L][S][S]   <- L is large at position 5 (left side)
 *   [ ][S][S]   <- L continues here
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class ExploreGridAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, ExploreGridAdapter.ExploreViewHolder>(PostDiffCallback()) {

    companion object {
        const val TYPE_SMALL = 0
        const val TYPE_LARGE = 1
        private const val TAG = "ExploreGridAdapter"
    }

    /** Currently playing video post ID */
    private var currentlyPlayingPostId: String? = null

    /** Map of postId to ExoPlayer for video posts */
    private val videoPlayers = mutableMapOf<String, ExoPlayer>()

    /** Global mute state */
    private var isMuted = true

    /**
     * Determines whether a position is a "large" tile.
     * Cycles every 10 items, positions 2 and 5 are large.
     */
    fun isLargeItem(position: Int): Boolean {
        val mod = position % 10
        return mod == 2 || mod == 5
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLargeItem(position)) TYPE_LARGE else TYPE_SMALL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExploreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_explore_grid, parent, false)
        return ExploreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExploreViewHolder, position: Int) {
        val isLarge = isLargeItem(position)
        
        // Calculate dimensions
        val screenWidth = holder.itemView.context.resources.displayMetrics.widthPixels
        val density = holder.itemView.context.resources.displayMetrics.density
        val spacingPx = (1 * density).toInt() // 1dp spacing
        val smallWidth = (screenWidth - spacingPx * 6) / 3  // 3 columns with spacing
        val smallHeight = smallWidth  // Square tiles
        val largeHeight = smallHeight * 2 + spacingPx * 2  // 2 rows tall
        
        val params = holder.itemView.layoutParams
        params.height = if (isLarge) largeHeight else smallHeight
        holder.itemView.layoutParams = params
        
        holder.bind(getItem(position), isLarge)
    }

    override fun onViewRecycled(holder: ExploreViewHolder) {
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

    inner class ExploreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_explore_image)
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.iv_video_thumbnail)
        private val videoPlayer: PlayerView = itemView.findViewById(R.id.video_player)
        private val videoIcon: ImageView = itemView.findViewById(R.id.iv_video_icon)
        private val videoDuration: TextView = itemView.findViewById(R.id.tv_video_duration)
        private val typeIcon: ImageView = itemView.findViewById(R.id.iv_type_icon)

        private var currentPostId: String? = null

        fun bind(post: Post, isLarge: Boolean) {
            currentPostId = post.postId

            if (post.isVideo && post.videoUrl.isNotBlank()) {
                // Video post
                setupVideoPost(post, isLarge)
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
            imageView.visibility = View.VISIBLE

            // Show carousel icon if multiple images
            if (post.isCarousel) {
                typeIcon.setImageResource(R.drawable.ic_collections)
                typeIcon.visibility = View.VISIBLE
            } else {
                typeIcon.visibility = View.GONE
            }

            // Load image
            if (post.postImageUrl.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(post.postImageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageView.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    imageView.setImageResource(0)
                    imageView.setBackgroundColor(0xFFEFEFEF.toInt())
                }
            } else {
                imageView.setImageResource(0)
                imageView.setBackgroundColor(0xFFEFEFEF.toInt())
            }
        }

        private fun setupVideoPost(post: Post, isLarge: Boolean) {
            // Hide image view
            imageView.visibility = View.GONE
            typeIcon.visibility = View.GONE

            // Show video views
            videoThumbnail.visibility = View.VISIBLE
            videoPlayer.visibility = View.VISIBLE
            videoIcon.visibility = View.VISIBLE

            // Adjust icon size for large tiles
            val iconSize = if (isLarge) 48 else 32
            val params = videoIcon.layoutParams
            val density = itemView.context.resources.displayMetrics.density
            params.width = (iconSize * density).toInt()
            params.height = (iconSize * density).toInt()
            videoIcon.layoutParams = params

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
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.postId == newItem.postId
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}
