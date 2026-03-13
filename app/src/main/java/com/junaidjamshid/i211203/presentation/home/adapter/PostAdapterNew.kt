package com.junaidjamshid.i211203.presentation.home.adapter

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.presentation.post.adapter.ImageCarouselAdapter

/**
 * Instagram-style Post Adapter with carousel, location, music, and VIDEO support.
 * Location and music alternate in the subtitle below the username.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PostAdapterNew(
    private val onLikeClick: (String) -> Unit,
    private val onCommentClick: (String) -> Unit,
    private val onShareClick: (String) -> Unit,
    private val onSaveClick: (String) -> Unit,
    private val onProfileClick: (String) -> Unit,
    private val onMenuClick: (Post) -> Unit,
    private val onVideoViewTracked: ((String) -> Unit)? = null
) : ListAdapter<Post, PostAdapterNew.PostViewHolder>(PostDiffCallback()) {

    companion object {
        private const val TAG = "PostAdapterNew"
    }

    /** Currently playing video post ID */
    private var currentlyPlayingPostId: String? = null

    /** Map of postId to ExoPlayer for video posts */
    private val videoPlayers = mutableMapOf<String, ExoPlayer>()

    /** Global mute state */
    private var isMuted = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
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
        if (currentlyPlayingPostId == postId) return // Already playing

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
     * Toggle global mute state
     */
    fun toggleMute(): Boolean {
        isMuted = !isMuted
        videoPlayers.values.forEach { it.volume = if (isMuted) 0f else 1f }
        return isMuted
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
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.username_text)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitle_text)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val postCarousel: ViewPager2 = itemView.findViewById(R.id.post_carousel)
        private val carouselCounter: TextView = itemView.findViewById(R.id.carousel_counter)
        private val carouselDots: LinearLayout = itemView.findViewById(R.id.carousel_dots)
        private val heartButton: ImageView = itemView.findViewById(R.id.heart)
        private val commentButton: ImageView = itemView.findViewById(R.id.comment)
        private val shareButton: ImageView = itemView.findViewById(R.id.send)
        private val saveButton: ImageView = itemView.findViewById(R.id.save)
        private val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        private val postCaption: TextView = itemView.findViewById(R.id.post_caption)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val menuButton: ImageView = itemView.findViewById(R.id.menu_dots)

        // Video views
        private val videoPlayerView: PlayerView = itemView.findViewById(R.id.video_player)
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        private val videoProgress: ProgressBar = itemView.findViewById(R.id.video_progress)
        private val muteIcon: ImageView = itemView.findViewById(R.id.mute_icon)
        private val videoDuration: TextView = itemView.findViewById(R.id.video_duration)
        private val videoViewsContainer: LinearLayout = itemView.findViewById(R.id.video_views_container)
        private val videoViewsCount: TextView = itemView.findViewById(R.id.video_views_count)
        private val playPauseButton: ImageView = itemView.findViewById(R.id.play_pause_button)

        private var carouselAdapter: ImageCarouselAdapter? = null
        private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
        private val subtitleHandler = Handler(Looper.getMainLooper())
        private var subtitleRunnable: Runnable? = null
        
        private var currentPostId: String? = null

        fun bind(post: Post) {
            currentPostId = post.postId
            usernameText.text = post.username
            likesCount.text = "${post.likesCount} likes"
            timestamp.text = getTimeAgo(post.timestamp)

            // Caption with bold username
            if (post.caption.isNotEmpty()) {
                val ssb = SpannableStringBuilder()
                ssb.append(post.username, StyleSpan(android.graphics.Typeface.BOLD), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.append(" ")
                ssb.append(post.caption)
                postCaption.text = ssb
                postCaption.visibility = View.VISIBLE
            } else {
                postCaption.visibility = View.GONE
            }

            // Subtitle: show location, music, or alternate both
            setupSubtitle(post)

            // Profile image
            loadProfileImage(post.userProfileImage, profileImage)

            // Media display: VIDEO vs IMAGE carousel vs single image
            if (post.isVideo && post.videoUrl.isNotBlank()) {
                setupVideoPost(post)
            } else {
                hideVideoViews()
                val allImages = post.allImages
                if (allImages.size > 1) {
                    setupCarousel(allImages)
                } else {
                    setupSingleImage(allImages, post.postImageUrl)
                }
            }

            // Like state
            heartButton.setImageResource(
                if (post.isLikedByCurrentUser) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
            
            // Save state
            saveButton.setImageResource(
                if (post.isSavedByCurrentUser) R.drawable.ic_bookmark_filled
                else R.drawable.ic_bookmark
            )

            // Click listeners
            heartButton.setOnClickListener { onLikeClick(post.postId) }
            commentButton.setOnClickListener { onCommentClick(post.postId) }
            shareButton.setOnClickListener { onShareClick(post.postId) }
            saveButton.setOnClickListener { onSaveClick(post.postId) }
            profileImage.setOnClickListener { onProfileClick(post.userId) }
            usernameText.setOnClickListener { onProfileClick(post.userId) }
            menuButton.setOnClickListener { onMenuClick(post) }
        }

        private fun setupVideoPost(post: Post) {
            Log.d(TAG, "Setting up video post: ${post.postId}, URL: ${post.videoUrl}")
            
            // Hide image views
            postImage.visibility = View.GONE
            postCarousel.visibility = View.GONE
            carouselCounter.visibility = View.GONE
            carouselDots.visibility = View.GONE

            // Show video views
            videoPlayerView.visibility = View.VISIBLE
            videoThumbnail.visibility = View.VISIBLE
            muteIcon.visibility = View.VISIBLE
            
            // Show duration if available
            if (post.videoDuration > 0) {
                videoDuration.text = post.formattedDuration
                videoDuration.visibility = View.VISIBLE
            } else {
                videoDuration.visibility = View.GONE
            }

            // Show views count if available
            if (post.viewsCount > 0) {
                videoViewsCount.text = formatViewsCount(post.viewsCount)
                videoViewsContainer.visibility = View.VISIBLE
            } else {
                videoViewsContainer.visibility = View.GONE
            }

            // Load thumbnail
            if (post.thumbnailUrl.isNotBlank()) {
                Glide.with(itemView.context)
                    .load(post.thumbnailUrl)
                    .centerCrop()
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(videoThumbnail)
            } else {
                videoThumbnail.setImageResource(R.drawable.bg_image_placeholder)
            }

            // Update mute icon
            muteIcon.setImageResource(
                if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
            )

            // Setup ExoPlayer
            setupExoPlayer(post)

            // Mute toggle click
            muteIcon.setOnClickListener {
                val newMuteState = toggleMute()
                muteIcon.setImageResource(
                    if (newMuteState) R.drawable.ic_volume_off else R.drawable.ic_volume_on
                )
            }

            // Play/pause on video tap
            videoPlayerView.setOnClickListener {
                val player = videoPlayers[post.postId]
                if (player?.isPlaying == true) {
                    pauseVideo(post.postId)
                    playPauseButton.setImageResource(R.drawable.ic_play_circle)
                    playPauseButton.alpha = 1f
                    playPauseButton.visibility = View.VISIBLE
                } else {
                    playVideo(post.postId)
                    playPauseButton.animate().alpha(0f).setDuration(300).start()
                }
            }
        }

        private fun setupExoPlayer(post: Post) {
            // Create or reuse player
            val player = videoPlayers.getOrPut(post.postId) {
                ExoPlayer.Builder(itemView.context).build().also { newPlayer ->
                    newPlayer.repeatMode = Player.REPEAT_MODE_ONE // Loop video
                    newPlayer.volume = if (isMuted) 0f else 1f
                    
                    newPlayer.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    videoProgress.visibility = View.VISIBLE
                                }
                                Player.STATE_READY -> {
                                    videoProgress.visibility = View.GONE
                                    // Hide thumbnail when video is ready to play
                                    if (newPlayer.isPlaying) {
                                        videoThumbnail.visibility = View.GONE
                                    }
                                }
                                Player.STATE_ENDED -> {
                                    // Track video view
                                    onVideoViewTracked?.invoke(post.postId)
                                }
                                Player.STATE_IDLE -> {
                                    videoProgress.visibility = View.GONE
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                videoThumbnail.visibility = View.GONE
                                playPauseButton.visibility = View.GONE
                            }
                        }
                    })
                }
            }

            // Set media item if not already set or different
            val mediaItem = MediaItem.fromUri(post.videoUrl)
            if (player.currentMediaItem?.localConfiguration?.uri?.toString() != post.videoUrl) {
                player.setMediaItem(mediaItem)
                player.prepare()
            }

            // Attach to PlayerView
            videoPlayerView.player = player
        }

        private fun hideVideoViews() {
            videoPlayerView.visibility = View.GONE
            videoThumbnail.visibility = View.GONE
            videoProgress.visibility = View.GONE
            muteIcon.visibility = View.GONE
            videoDuration.visibility = View.GONE
            videoViewsContainer.visibility = View.GONE
            playPauseButton.visibility = View.GONE
            
            // Detach player
            videoPlayerView.player = null
        }

        private fun formatViewsCount(count: Int): String {
            return when {
                count >= 1_000_000 -> "${count / 1_000_000}M"
                count >= 1_000 -> "${count / 1_000}K"
                else -> count.toString()
            }
        }

        /**
         * Shows location and/or music below username, alternating if both exist.
         * Exactly like Instagram: location for a few seconds, then music, cycling.
         */
        private fun setupSubtitle(post: Post) {
            // Stop any previous animation
            subtitleRunnable?.let { subtitleHandler.removeCallbacks(it) }
            subtitleRunnable = null

            val hasLoc = post.hasLocation
            val hasMus = post.hasMusic

            if (!hasLoc && !hasMus) {
                subtitleText.visibility = View.GONE
                return
            }

            subtitleText.visibility = View.VISIBLE

            if (hasLoc && hasMus) {
                // Alternate between location and music every 3 seconds
                var showingLocation = true
                subtitleText.text = post.location

                subtitleRunnable = object : Runnable {
                    override fun run() {
                        showingLocation = !showingLocation
                        if (showingLocation) {
                            subtitleText.text = post.location
                        } else {
                            subtitleText.text = "\u266B ${post.musicName} · ${post.musicArtist}"
                        }
                        subtitleHandler.postDelayed(this, 10000)
                    }
                }
                subtitleHandler.postDelayed(subtitleRunnable!!, 10000)
            } else if (hasLoc) {
                subtitleText.text = post.location
            } else {
                subtitleText.text = "\u266B ${post.musicName} · ${post.musicArtist}"
            }
        }

        private fun setupCarousel(images: List<String>) {
            postImage.visibility = View.GONE
            postCarousel.visibility = View.VISIBLE
            carouselCounter.visibility = View.VISIBLE

            // Remove old callback
            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }

            if (carouselAdapter == null) {
                carouselAdapter = ImageCarouselAdapter()
                postCarousel.adapter = carouselAdapter
            }

            // Fix for ViewPager2 inside RecyclerView - reduce sensitivity
            postCarousel.getChildAt(0)?.let { innerRecycler ->
                if (innerRecycler is RecyclerView) {
                    innerRecycler.isNestedScrollingEnabled = false
                    innerRecycler.overScrollMode = View.OVER_SCROLL_NEVER
                }
            }

            carouselAdapter?.submitBase64Images(images)
            postCarousel.setCurrentItem(0, false)

            carouselCounter.text = "1/${images.size}"
            setupDots(images.size)

            pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    carouselCounter.text = "${position + 1}/${images.size}"
                    updateDots(position)
                }
            }
            postCarousel.registerOnPageChangeCallback(pageChangeCallback!!)
        }

        private fun setupSingleImage(allImages: List<String>, fallbackUrl: String) {
            postCarousel.visibility = View.GONE
            carouselCounter.visibility = View.GONE
            carouselDots.visibility = View.GONE
            postImage.visibility = View.VISIBLE

            val imageData = if (allImages.isNotEmpty()) allImages[0] else fallbackUrl
            loadBase64Image(imageData, postImage, 0)
        }

        private fun setupDots(count: Int) {
            carouselDots.removeAllViews()
            if (count <= 1) {
                carouselDots.visibility = View.GONE
                return
            }
            carouselDots.visibility = View.VISIBLE
            for (i in 0 until count) {
                val dot = ImageView(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(6.dpToPx(), 6.dpToPx()).apply {
                        marginStart = 3.dpToPx()
                        marginEnd = 3.dpToPx()
                    }
                    setImageResource(if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
                }
                carouselDots.addView(dot)
            }
        }

        private fun updateDots(selectedPos: Int) {
            for (i in 0 until carouselDots.childCount) {
                (carouselDots.getChildAt(i) as? ImageView)?.setImageResource(
                    if (i == selectedPos) R.drawable.dot_active else R.drawable.dot_inactive
                )
            }
        }

        private fun loadBase64Image(base64: String, imageView: ImageView, fallbackRes: Int) {
            if (base64.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        return
                    }
                } catch (_: Exception) { }
            }
            if (fallbackRes != 0) imageView.setImageResource(fallbackRes)
        }

        /**
         * Load profile image - supports both base64 and URL
         */
        private fun loadProfileImage(imageData: String, imageView: ImageView) {
            if (imageData.isBlank()) {
                imageView.setImageResource(R.drawable.default_profile)
                return
            }
            
            // Check if it's a URL
            if (imageData.startsWith("http://") || imageData.startsWith("https://")) {
                Glide.with(itemView.context)
                    .load(imageData)
                    .circleCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(imageView)
            } else {
                // Assume base64
                loadBase64Image(imageData, imageView, R.drawable.default_profile)
            }
        }

        private fun Int.dpToPx(): Int =
            (this * itemView.context.resources.displayMetrics.density).toInt()

        private fun getTimeAgo(timestamp: Long): String {
            val diff = System.currentTimeMillis() - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7

            return when {
                weeks > 0 -> "${weeks} weeks"
                days > 0 -> "${days} days"
                hours > 0 -> "${hours} hours"
                minutes > 0 -> "${minutes} minutes"
                else -> "now"
            }
        }

        fun cleanup() {
            subtitleRunnable?.let { subtitleHandler.removeCallbacks(it) }
            subtitleRunnable = null
            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }
            pageChangeCallback = null
            
            // Detach video player from view (don't release - managed by adapter)
            videoPlayerView.player = null
            currentPostId = null
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) =
            oldItem.postId == newItem.postId

        override fun areContentsTheSame(oldItem: Post, newItem: Post) =
            oldItem == newItem
    }
}
