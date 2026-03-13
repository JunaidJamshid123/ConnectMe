package com.junaidjamshid.i211203.presentation.home.adapter

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.presentation.home.video.ExoPlayerPool
import com.junaidjamshid.i211203.presentation.post.adapter.ImageCarouselAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Sealed class representing the different items in the home feed.
 */
sealed class HomeFeedItem {
    data class PostItem(val post: Post) : HomeFeedItem()
    data class SuggestionsItem(val suggestions: List<User>) : HomeFeedItem()
}

/**
 * Multi-type adapter for the home feed that handles both posts and suggestion rows.
 * Suggestions appear inline between posts similar to Instagram.
 * Supports image posts, carousels, and video/reel posts with auto-play.
 */
class HomeFeedAdapter(
    private val onLikeClick: (String) -> Unit,
    private val onCommentClick: (String) -> Unit,
    private val onShareClick: (String) -> Unit,
    private val onSaveClick: (String) -> Unit,
    private val onProfileClick: (String) -> Unit,
    private val onMenuClick: (Post) -> Unit,
    private val onFollowClick: (String) -> Unit,
    private val onSeeAllSuggestionsClick: () -> Unit,
    private val onMuteToggle: (() -> Unit)? = null,
    private val onVideoClick: ((String) -> Unit)? = null
) : ListAdapter<HomeFeedItem, RecyclerView.ViewHolder>(FeedDiffCallback()) {

    companion object {
        private const val TYPE_POST = 0
        private const val TYPE_SUGGESTIONS = 1
    }

    /** Set of user IDs the current user is following */
    var followingUserIds: Set<String> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /** Current user ID to hide follow button on own posts */
    var currentUserId: String = ""

    /** ExoPlayer pool for video playback */
    var playerPool: ExoPlayerPool? = null

    /** Coroutine scope for progress updates */
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeFeedItem.PostItem -> TYPE_POST
            is HomeFeedItem.SuggestionsItem -> TYPE_SUGGESTIONS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_POST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_post, parent, false)
                PostViewHolder(view)
            }
            TYPE_SUGGESTIONS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_suggestions_row, parent, false)
                SuggestionsViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeFeedItem.PostItem -> (holder as PostViewHolder).bind(item.post)
            is HomeFeedItem.SuggestionsItem -> (holder as SuggestionsViewHolder).bind(item.suggestions)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        
        val item = getItem(position)
        if (item is HomeFeedItem.PostItem && holder is PostViewHolder) {
            val changes = payloads.filterIsInstance<Set<*>>().flatten().toSet()
            holder.bindPartial(item.post, changes)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PostViewHolder) holder.cleanup()
    }

    // ========================= POST VIEW HOLDER =========================

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val usernameText: TextView = itemView.findViewById(R.id.username_text)
        private val followButton: TextView = itemView.findViewById(R.id.btn_follow_user)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitle_text)
        private val postImage: ImageView = itemView.findViewById(R.id.post_image)
        private val postImageContainer: View = itemView.findViewById(R.id.post_image_container)
        private val doubleTapHeart: LottieAnimationView = itemView.findViewById(R.id.double_tap_heart)
        private val postCarousel: ViewPager2 = itemView.findViewById(R.id.post_carousel)
        private val carouselCounter: TextView = itemView.findViewById(R.id.carousel_counter)
        private val carouselDots: LinearLayout = itemView.findViewById(R.id.carousel_dots)
        private val heartButton: ImageView = itemView.findViewById(R.id.heart)
        private val commentButton: ImageView = itemView.findViewById(R.id.comment)
        private val shareButton: ImageView = itemView.findViewById(R.id.send)
        private val saveButton: ImageView = itemView.findViewById(R.id.save)
        private val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        private val postCaption: TextView = itemView.findViewById(R.id.post_caption)
        private val viewComments: TextView = itemView.findViewById(R.id.view_comments)
        private val addCommentRow: View = itemView.findViewById(R.id.add_comment_row)
        private val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        private val menuButton: ImageView = itemView.findViewById(R.id.menu_dots)

        // Video player components
        private val videoPlayer: PlayerView = itemView.findViewById(R.id.video_player)
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.video_thumbnail)
        private val videoProgress: ProgressBar = itemView.findViewById(R.id.video_progress)
        private val muteIcon: ImageView = itemView.findViewById(R.id.mute_icon)
        private val videoDuration: TextView = itemView.findViewById(R.id.video_duration)
        private val videoViewsContainer: View = itemView.findViewById(R.id.video_views_container)
        private val videoViewsCount: TextView = itemView.findViewById(R.id.video_views_count)
        private val playPauseButton: ImageView = itemView.findViewById(R.id.play_pause_button)

        private var carouselAdapter: ImageCarouselAdapter? = null
        private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
        private val subtitleHandler = Handler(Looper.getMainLooper())
        private var subtitleRunnable: Runnable? = null

        private var currentPostId: String = ""
        private var progressJob: Job? = null
        private var playerListener: Player.Listener? = null

        // Gesture detector for double-tap to like
        private val gestureDetector = GestureDetectorCompat(itemView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onLikeClick(currentPostId)
                    showDoubleTapLikeAnimation()
                    return true
                }
            }
        )

        private fun showDoubleTapLikeAnimation() {
            doubleTapHeart.visibility = View.VISIBLE
            doubleTapHeart.playAnimation()
            doubleTapHeart.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    doubleTapHeart.visibility = View.GONE
                    doubleTapHeart.removeAllAnimatorListeners()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    doubleTapHeart.visibility = View.GONE
                    doubleTapHeart.removeAllAnimatorListeners()
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
        }

        @SuppressLint("ClickableViewAccessibility")
        fun bind(post: Post) {
            currentPostId = post.postId
            usernameText.text = post.username
            likesCount.text = "${post.likesCount} likes"
            timestamp.text = getTimeAgo(post.timestamp)

            // Setup double-tap gesture on post image container
            postImageContainer.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            // Follow button logic — show "Follow" for users not followed, hide for self or already followed
            val isOwnPost = post.userId == currentUserId
            val isFollowing = followingUserIds.contains(post.userId)
            if (!isOwnPost && !isFollowing) {
                followButton.visibility = View.VISIBLE
                followButton.text = "Follow"
                followButton.setTextColor(0xFF0095F6.toInt())
                followButton.setOnClickListener { onFollowClick(post.userId) }
            } else {
                followButton.visibility = View.GONE
            }

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
            loadBase64Image(post.userProfileImage, profileImage, R.drawable.default_profile)

            // Media display: video, carousel, or single image
            if (post.isVideo) {
                setupVideo(post)
            } else {
                // Hide video components
                hideVideoComponents()
                
                // Image display: carousel vs single
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

            // View all comments
            if (post.commentsCount > 0) {
                viewComments.visibility = View.VISIBLE
                viewComments.text = if (post.commentsCount == 1) {
                    "View 1 comment"
                } else {
                    "View all ${post.commentsCount} comments"
                }
            } else {
                viewComments.visibility = View.GONE
            }

            // Click listeners
            heartButton.setOnClickListener { 
                animateHeartButton()
                onLikeClick(post.postId) 
            }
            commentButton.setOnClickListener { onCommentClick(post.postId) }
            shareButton.setOnClickListener { onShareClick(post.postId) }
            saveButton.setOnClickListener { 
                animateSaveButton()
                onSaveClick(post.postId) 
            }
            profileImage.setOnClickListener { onProfileClick(post.userId) }
            usernameText.setOnClickListener { onProfileClick(post.userId) }
            menuButton.setOnClickListener { onMenuClick(post) }
            viewComments.setOnClickListener { onCommentClick(post.postId) }
            addCommentRow.setOnClickListener { onCommentClick(post.postId) }
        }
        
        private fun animateHeartButton() {
            heartButton.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(100)
                .withEndAction {
                    heartButton.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.OvershootInterpolator(3f))
                        .withEndAction {
                            heartButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                }
                .start()
        }
        
        private fun animateSaveButton() {
            saveButton.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction {
                    saveButton.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(150)
                        .setInterpolator(android.view.animation.OvershootInterpolator(2f))
                        .withEndAction {
                            saveButton.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                        }
                        .start()
                }
                .start()
        }
        
        /**
         * Partial bind for efficient updates - only updates changed UI elements.
         */
        fun bindPartial(post: Post, changes: Set<*>) {
            if (changes.contains("like")) {
                heartButton.setImageResource(
                    if (post.isLikedByCurrentUser) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart_outline
                )
            }
            if (changes.contains("save")) {
                saveButton.setImageResource(
                    if (post.isSavedByCurrentUser) R.drawable.ic_bookmark_filled
                    else R.drawable.ic_bookmark
                )
            }
            if (changes.contains("likesCount")) {
                likesCount.text = "${post.likesCount} likes"
            }
            if (changes.contains("commentsCount")) {
                if (post.commentsCount > 0) {
                    viewComments.visibility = View.VISIBLE
                    viewComments.text = if (post.commentsCount == 1) {
                        "View 1 comment"
                    } else {
                        "View all ${post.commentsCount} comments"
                    }
                } else {
                    viewComments.visibility = View.GONE
                }
            }
        }

        private fun setupSubtitle(post: Post) {
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

            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }

            if (carouselAdapter == null) {
                carouselAdapter = ImageCarouselAdapter()
                postCarousel.adapter = carouselAdapter
            }

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

        // ========================= VIDEO METHODS =========================

        private fun setupVideo(post: Post) {
            Log.d("HomeFeedAdapter", "Setting up video for post ${post.postId}")
            Log.d("HomeFeedAdapter", "  videoUrl: ${post.videoUrl}")
            Log.d("HomeFeedAdapter", "  thumbnailUrl: ${post.thumbnailUrl}")
            Log.d("HomeFeedAdapter", "  displayThumbnail: ${post.displayThumbnail}")
            
            // Hide image components
            postImage.visibility = View.GONE
            postCarousel.visibility = View.GONE
            carouselCounter.visibility = View.GONE
            carouselDots.visibility = View.GONE

            // Show video components
            videoPlayer.visibility = View.VISIBLE
            videoThumbnail.visibility = View.VISIBLE
            videoProgress.visibility = View.VISIBLE
            muteIcon.visibility = View.VISIBLE
            
            // Show duration badge
            if (post.formattedDuration.isNotEmpty()) {
                videoDuration.visibility = View.VISIBLE
                videoDuration.text = post.formattedDuration
            } else {
                videoDuration.visibility = View.GONE
            }

            // Show views count for videos
            if (post.viewsCount > 0) {
                videoViewsContainer.visibility = View.VISIBLE
                videoViewsCount.text = formatViewCount(post.viewsCount)
            } else {
                videoViewsContainer.visibility = View.GONE
            }

            // Load thumbnail - now supports both URL and base64
            val thumbnailSource = post.displayThumbnail
            if (thumbnailSource.isNotBlank()) {
                if (thumbnailSource.startsWith("http://") || thumbnailSource.startsWith("https://")) {
                    // Load from URL using Glide
                    Glide.with(itemView.context)
                        .load(thumbnailSource)
                        .centerCrop()
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .into(videoThumbnail)
                } else {
                    // Try as base64
                    loadBase64Image(thumbnailSource, videoThumbnail, R.drawable.bg_image_placeholder)
                }
            } else {
                videoThumbnail.setImageResource(R.drawable.bg_image_placeholder)
            }

            // Setup player using the pool
            playerPool?.let { pool ->
                Log.d("HomeFeedAdapter", "Preparing video in pool for ${post.postId}")
                pool.prepareVideo(post.postId, post.videoUrl)
                videoPlayer.player = pool.getPlayerForPost(post.postId)
                
                // Setup mute icon state
                updateMuteIcon(pool.isMuted.value)
                
                // Setup player listener for progress updates
                setupPlayerListener(post.postId)
            } ?: run {
                Log.w("HomeFeedAdapter", "PlayerPool is null! Video won't play.")
            }

            // Mute icon click - toggle mute
            muteIcon.setOnClickListener {
                playerPool?.toggleMute()
                updateMuteIcon(playerPool?.isMuted?.value ?: true)
                onMuteToggle?.invoke()
            }

            // Video click - toggle play/pause or navigate
            videoPlayer.setOnClickListener {
                onVideoClick?.invoke(post.postId)
            }
        }

        private fun setupPlayerListener(postId: String) {
            // Remove old listener
            playerListener?.let {
                playerPool?.removePlayerListener(postId, it)
            }

            playerListener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            videoThumbnail.visibility = View.GONE
                            startProgressUpdates(postId)
                        }
                        Player.STATE_BUFFERING -> {
                            // Could show loading indicator
                        }
                        Player.STATE_ENDED -> {
                            // Video looped (repeatMode is ONE)
                        }
                        Player.STATE_IDLE -> {
                            videoThumbnail.visibility = View.VISIBLE
                            stopProgressUpdates()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        videoThumbnail.visibility = View.GONE
                        playPauseButton.visibility = View.GONE
                    } else {
                        playPauseButton.alpha = 0.7f
                        playPauseButton.visibility = View.VISIBLE
                    }
                }
            }

            playerPool?.addPlayerListener(postId, playerListener!!)
        }

        private fun startProgressUpdates(postId: String) {
            progressJob?.cancel()
            progressJob = coroutineScope.launch {
                while (isActive) {
                    val player = playerPool?.getPlayerForPost(postId)
                    if (player != null && player.duration > 0) {
                        val progress = (player.currentPosition * 100 / player.duration).toInt()
                        videoProgress.progress = progress
                    }
                    delay(100)
                }
            }
        }

        private fun stopProgressUpdates() {
            progressJob?.cancel()
            progressJob = null
        }

        private fun hideVideoComponents() {
            videoPlayer.visibility = View.GONE
            videoPlayer.player = null
            videoThumbnail.visibility = View.GONE
            videoProgress.visibility = View.GONE
            muteIcon.visibility = View.GONE
            videoDuration.visibility = View.GONE
            videoViewsContainer.visibility = View.GONE
            playPauseButton.visibility = View.GONE
            stopProgressUpdates()
        }

        private fun updateMuteIcon(isMuted: Boolean) {
            muteIcon.setImageResource(
                if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
            )
        }

        private fun formatViewCount(count: Int): String {
            return when {
                count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }

        /** Attach player for this post (called by VideoAutoPlayManager) */
        fun attachPlayer(postId: String) {
            if (currentPostId == postId) {
                playerPool?.let { pool ->
                    videoPlayer.player = pool.getPlayerForPost(postId)
                }
            }
        }

        /** Detach player from this view holder */
        fun detachPlayer() {
            videoPlayer.player = null
            stopProgressUpdates()
        }

        fun cleanup() {
            subtitleRunnable?.let { subtitleHandler.removeCallbacks(it) }
            subtitleRunnable = null
            pageChangeCallback?.let { postCarousel.unregisterOnPageChangeCallback(it) }
            pageChangeCallback = null
            // Video cleanup
            playerListener?.let {
                playerPool?.removePlayerListener(currentPostId, it)
            }
            playerListener = null
            stopProgressUpdates()
            videoPlayer.player = null
        }
    }

    // ========================= SUGGESTIONS VIEW HOLDER =========================

    inner class SuggestionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val suggestionsRecyclerView: RecyclerView = itemView.findViewById(R.id.suggestions_recycler)
        private val seeAllButton: TextView = itemView.findViewById(R.id.btn_see_all)

        fun bind(suggestions: List<User>) {
            val adapter = SuggestionCardAdapter(
                onFollowClick = { userId -> onFollowClick(userId) },
                onDismissClick = { /* handled by ViewModel later */ },
                onProfileClick = { userId -> onProfileClick(userId) }
            )
            adapter.followingUserIds = followingUserIds
            suggestionsRecyclerView.layoutManager =
                LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            suggestionsRecyclerView.adapter = adapter
            adapter.submitList(suggestions)

            seeAllButton.setOnClickListener { onSeeAllSuggestionsClick() }
        }
    }

    // ========================= HELPERS =========================

    private fun loadBase64Image(base64: String, imageView: ImageView, fallbackRes: Int) {
        if (base64.isEmpty()) {
            if (fallbackRes != 0) imageView.setImageResource(fallbackRes)
            return
        }
        
        // Check if it's a URL (for Supabase Storage images)
        if (base64.startsWith("http://") || base64.startsWith("https://")) {
            Glide.with(imageView.context)
                .load(base64)
                .centerCrop()
                .into(imageView)
            return
        }
        
        // Otherwise try to decode as base64
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return
            }
        } catch (e: Exception) {
            Log.w("HomeFeedAdapter", "Failed to decode image: ${e.message}")
        }
        
        if (fallbackRes != 0) imageView.setImageResource(fallbackRes)
    }

    /**
     * Load image from URL using Glide (for video thumbnails and profile pics)
     */
    private fun loadUrlImage(url: String, imageView: ImageView, fallbackRes: Int = 0) {
        if (url.isEmpty()) {
            if (fallbackRes != 0) imageView.setImageResource(fallbackRes)
            return
        }
        
        Glide.with(imageView.context)
            .load(url)
            .centerCrop()
            .apply {
                if (fallbackRes != 0) {
                    placeholder(fallbackRes)
                    error(fallbackRes)
                }
            }
            .into(imageView)
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7

        return when {
            weeks > 0 -> "$weeks weeks"
            days > 0 -> "$days days"
            hours > 0 -> "$hours hours"
            minutes > 0 -> "$minutes minutes"
            else -> "now"
        }
    }

    private fun Int.dpToPx(): Int =
        (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

    // ========================= PUBLIC HELPERS =========================

    /**
     * Get Post at a specific position (for VideoAutoPlayManager).
     * Returns null if position is invalid or item is not a PostItem.
     */
    fun getPostAtPosition(position: Int): Post? {
        if (position < 0 || position >= itemCount) return null
        val item = getItem(position)
        return (item as? HomeFeedItem.PostItem)?.post
    }

    // ========================= DIFF CALLBACK =========================

    class FeedDiffCallback : DiffUtil.ItemCallback<HomeFeedItem>() {
        override fun areItemsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
            return when {
                oldItem is HomeFeedItem.PostItem && newItem is HomeFeedItem.PostItem ->
                    oldItem.post.postId == newItem.post.postId
                oldItem is HomeFeedItem.SuggestionsItem && newItem is HomeFeedItem.SuggestionsItem -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeFeedItem, newItem: HomeFeedItem): Boolean {
            return oldItem == newItem
        }
        
        override fun getChangePayload(oldItem: HomeFeedItem, newItem: HomeFeedItem): Any? {
            if (oldItem is HomeFeedItem.PostItem && newItem is HomeFeedItem.PostItem) {
                val oldPost = oldItem.post
                val newPost = newItem.post
                val changes = mutableSetOf<String>()
                
                if (oldPost.isLikedByCurrentUser != newPost.isLikedByCurrentUser) {
                    changes.add("like")
                }
                if (oldPost.isSavedByCurrentUser != newPost.isSavedByCurrentUser) {
                    changes.add("save")
                }
                if (oldPost.likesCount != newPost.likesCount) {
                    changes.add("likesCount")
                }
                if (oldPost.commentsCount != newPost.commentsCount) {
                    changes.add("commentsCount")
                }
                
                return if (changes.isNotEmpty()) changes else null
            }
            return null
        }
    }
}
