package com.junaidjamshid.i211203.presentation.home.video

import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.presentation.home.adapter.HomeFeedItem

/**
 * Manager for handling auto-play of videos in the home feed.
 * Automatically plays the most visible video post when scrolling stops.
 * 
 * Features:
 * - Auto-play video when it becomes visible in viewport
 * - Auto-pause when scrolling or video goes off-screen
 * - Track video views after minimum watch time
 */
class VideoAutoPlayManager(
    private val recyclerView: RecyclerView,
    private val playerPool: ExoPlayerPool,
    private val onVideoViewTracked: (String) -> Unit  // Callback when view should be recorded
) {
    
    companion object {
        private const val TAG = "VideoAutoPlayManager"
        private const val VISIBILITY_THRESHOLD = 0.6f  // 60% visible to trigger play
        private const val VIEW_TRACK_TIME_MS = 3000L   // Track view after 3 seconds
    }
    
    /** The post ID of the currently playing video */
    private var currentlyPlayingPostId: String? = null
    
    /** Watch time tracker for view counting */
    private val watchStartTimes = mutableMapOf<String, Long>()
    
    /** Posts that have already been tracked as viewed */
    private val trackedViews = mutableSetOf<String>()
    
    /** Adapter to get post data */
    private var adapter: RecyclerView.Adapter<*>? = null
    
    /** Function to get post at position */
    private var getPostAtPosition: ((Int) -> Post?)? = null
    
    init {
        setupScrollListener()
    }
    
    /**
     * Set the adapter and post getter function.
     */
    fun setAdapter(
        adapter: RecyclerView.Adapter<*>,
        getPost: (Int) -> Post?
    ) {
        this.adapter = adapter
        this.getPostAtPosition = getPost
    }
    
    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                super.onScrollStateChanged(rv, newState)
                
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        // Find and play the most visible video
                        playMostVisibleVideo()
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING -> {
                        // Optionally pause during fast scrolling
                        // Keeping playback during slow scroll for better UX
                    }
                }
            }
            
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                
                // Check if current video is still visible
                currentlyPlayingPostId?.let { postId ->
                    if (!isVideoVisible(postId)) {
                        pauseVideo(postId)
                    }
                }
            }
        })
    }
    
    /**
     * Find and play the most visible video in the viewport.
     */
    fun playMostVisibleVideo() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        
        if (firstVisible == RecyclerView.NO_POSITION) return
        
        var bestPostId: String? = null
        var bestVisibility = 0f
        
        for (position in firstVisible..lastVisible) {
            val post = getPostAtPosition?.invoke(position) ?: continue
            
            if (!post.isVideo) continue
            
            val visibility = getItemVisibility(position)
            if (visibility > bestVisibility && visibility >= VISIBILITY_THRESHOLD) {
                bestVisibility = visibility
                bestPostId = post.postId
            }
        }
        
        // Play the most visible video, pause others
        if (bestPostId != null && bestPostId != currentlyPlayingPostId) {
            currentlyPlayingPostId?.let { pauseVideo(it) }
            playVideo(bestPostId)
        } else if (bestPostId == null) {
            currentlyPlayingPostId?.let { pauseVideo(it) }
        }
    }
    
    /**
     * Calculate how much of an item is visible (0.0 to 1.0).
     */
    private fun getItemVisibility(position: Int): Float {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return 0f
        val view = layoutManager.findViewByPosition(position) ?: return 0f
        
        val rvTop = recyclerView.top
        val rvBottom = recyclerView.bottom
        val rvHeight = rvBottom - rvTop
        
        val viewTop = view.top
        val viewBottom = view.bottom
        val viewHeight = viewBottom - viewTop
        
        if (viewHeight == 0) return 0f
        
        val visibleTop = maxOf(viewTop, rvTop)
        val visibleBottom = minOf(viewBottom, rvBottom)
        val visibleHeight = maxOf(0, visibleBottom - visibleTop)
        
        return visibleHeight.toFloat() / viewHeight.toFloat()
    }
    
    /**
     * Check if a video post is currently visible.
     */
    private fun isVideoVisible(postId: String): Boolean {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        
        for (position in firstVisible..lastVisible) {
            val post = getPostAtPosition?.invoke(position) ?: continue
            if (post.postId == postId) {
                return getItemVisibility(position) >= VISIBILITY_THRESHOLD
            }
        }
        return false
    }
    
    /**
     * Play a video by post ID.
     */
    private fun playVideo(postId: String) {
        currentlyPlayingPostId = postId
        playerPool.play(postId)
        startWatchTimer(postId)
        Log.d(TAG, "Playing video: $postId")
    }
    
    /**
     * Pause a video by post ID.
     */
    private fun pauseVideo(postId: String) {
        playerPool.pause(postId)
        stopWatchTimer(postId)
        if (currentlyPlayingPostId == postId) {
            currentlyPlayingPostId = null
        }
        Log.d(TAG, "Paused video: $postId")
    }
    
    /**
     * Start tracking watch time for view counting.
     */
    private fun startWatchTimer(postId: String) {
        if (trackedViews.contains(postId)) return
        watchStartTimes[postId] = System.currentTimeMillis()
    }
    
    /**
     * Stop tracking watch time and record view if threshold met.
     */
    private fun stopWatchTimer(postId: String) {
        val startTime = watchStartTimes.remove(postId) ?: return
        val watchTime = System.currentTimeMillis() - startTime
        
        if (watchTime >= VIEW_TRACK_TIME_MS && !trackedViews.contains(postId)) {
            trackedViews.add(postId)
            onVideoViewTracked(postId)
            Log.d(TAG, "Tracked view for video: $postId (watched ${watchTime}ms)")
        }
    }
    
    /**
     * Prepare a video for playback (preload).
     */
    fun prepareVideo(postId: String, videoUrl: String) {
        playerPool.prepareVideo(postId, videoUrl)
    }
    
    /**
     * Pause all videos. Call when fragment goes to background.
     */
    fun pauseAll() {
        currentlyPlayingPostId?.let { stopWatchTimer(it) }
        playerPool.pauseAll()
        currentlyPlayingPostId = null
    }
    
    /**
     * Resume auto-play. Call when fragment returns to foreground.
     */
    fun resume() {
        playMostVisibleVideo()
    }
    
    /**
     * Toggle mute state for all videos.
     */
    fun toggleMute() {
        playerPool.toggleMute()
    }
    
    /**
     * Check if currently muted.
     */
    fun isMuted(): Boolean = playerPool.isMuted.value
    
    /**
     * Get the currently playing post ID.
     */
    fun getCurrentlyPlayingPostId(): String? = currentlyPlayingPostId
    
    /**
     * Clean up resources. Call when fragment is destroyed.
     */
    fun release() {
        pauseAll()
        watchStartTimes.clear()
        trackedViews.clear()
        playerPool.releaseAll()
    }
    
    /**
     * Handle manual play/pause toggle for a specific video.
     */
    fun togglePlayPause(postId: String) {
        if (playerPool.isPlaying(postId)) {
            pauseVideo(postId)
        } else {
            // Pause current video if different
            currentlyPlayingPostId?.let { 
                if (it != postId) pauseVideo(it)
            }
            playVideo(postId)
        }
    }
}
