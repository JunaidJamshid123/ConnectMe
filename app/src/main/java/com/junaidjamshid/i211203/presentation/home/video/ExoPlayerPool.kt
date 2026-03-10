package com.junaidjamshid.i211203.presentation.home.video

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pool manager for ExoPlayer instances to enable efficient video playback
 * across multiple video posts in the feed.
 * 
 * Manages a small pool of players (default 3) that can be reused across
 * different video posts, minimizing memory usage and initialization overhead.
 */
class ExoPlayerPool(
    private val context: Context,
    private val poolSize: Int = 3
) {
    
    companion object {
        private const val TAG = "ExoPlayerPool"
        
        // Load control parameters for smooth playback
        private const val MIN_BUFFER_MS = 5000       // 5 seconds min buffer
        private const val MAX_BUFFER_MS = 15000      // 15 seconds max buffer
        private const val BUFFER_FOR_PLAYBACK_MS = 1000
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 2000
    }
    
    /** Global mute state - shared across all video players */
    private val _isMuted = MutableStateFlow(true)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    /** Map of player index to post ID currently using it */
    private val playerAssignments = mutableMapOf<Int, String>()
    
    /** The actual player pool */
    private val players: List<ExoPlayer> by lazy {
        List(poolSize) { createPlayer() }
    }
    
    /** Active players that are currently visible/playing */
    private val activePlayers = mutableSetOf<Int>()
    
    /**
     * Create a new ExoPlayer instance with optimized settings for feed videos.
     */
    private fun createPlayer(): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
        
        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                volume = 0f  // Start muted
                repeatMode = Player.REPEAT_MODE_ONE  // Loop videos
                playWhenReady = false
            }
    }
    
    /**
     * Get an available player for a specific post.
     * If the post already has a player assigned, returns that player.
     * Otherwise, assigns the least recently used player.
     * 
     * @param postId The unique ID of the post requesting a player
     * @return An ExoPlayer instance ready for use
     */
    fun getPlayer(postId: String): ExoPlayer {
        // Check if post already has a player
        playerAssignments.entries.find { it.value == postId }?.let { entry ->
            Log.d(TAG, "Returning existing player ${entry.key} for post $postId")
            return players[entry.key]
        }
        
        // Find an unassigned player
        val availableIndex = (0 until poolSize).find { !playerAssignments.containsKey(it) }
        
        if (availableIndex != null) {
            playerAssignments[availableIndex] = postId
            Log.d(TAG, "Assigning new player $availableIndex to post $postId")
            return players[availableIndex]
        }
        
        // All players assigned - find least recently active one
        val lruIndex = (0 until poolSize).find { it !in activePlayers }
            ?: 0  // Fallback to first player
        
        // Release old assignment
        val oldPostId = playerAssignments[lruIndex]
        players[lruIndex].stop()
        players[lruIndex].clearMediaItems()
        
        playerAssignments[lruIndex] = postId
        Log.d(TAG, "Recycling player $lruIndex from post $oldPostId to $postId")
        
        return players[lruIndex]
    }
    
    /**
     * Prepare a player with a video URL.
     * 
     * @param postId The post ID
     * @param videoUrl The video URL to load
     */
    fun prepareVideo(postId: String, videoUrl: String) {
        val player = getPlayer(postId)
        val mediaItem = MediaItem.fromUri(videoUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.volume = if (_isMuted.value) 0f else 1f
        Log.d(TAG, "Prepared video for post $postId: $videoUrl")
    }
    
    /**
     * Start playback for a specific post.
     */
    fun play(postId: String) {
        val index = playerAssignments.entries.find { it.value == postId }?.key ?: return
        activePlayers.add(index)
        players[index].apply {
            volume = if (_isMuted.value) 0f else 1f
            playWhenReady = true
        }
        Log.d(TAG, "Playing video for post $postId")
    }
    
    /**
     * Pause playback for a specific post.
     */
    fun pause(postId: String) {
        val index = playerAssignments.entries.find { it.value == postId }?.key ?: return
        activePlayers.remove(index)
        players[index].playWhenReady = false
        Log.d(TAG, "Paused video for post $postId")
    }
    
    /**
     * Pause all video playback.
     */
    fun pauseAll() {
        players.forEach { it.playWhenReady = false }
        activePlayers.clear()
        Log.d(TAG, "Paused all videos")
    }
    
    /**
     * Release a player assignment for a specific post.
     */
    fun release(postId: String) {
        val index = playerAssignments.entries.find { it.value == postId }?.key ?: return
        players[index].stop()
        players[index].clearMediaItems()
        activePlayers.remove(index)
        playerAssignments.remove(index)
        Log.d(TAG, "Released player for post $postId")
    }
    
    /**
     * Toggle global mute state.
     */
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        val volume = if (_isMuted.value) 0f else 1f
        players.forEach { it.volume = volume }
        Log.d(TAG, "Toggled mute: ${_isMuted.value}")
    }
    
    /**
     * Set mute state.
     */
    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        val volume = if (muted) 0f else 1f
        players.forEach { it.volume = volume }
    }
    
    /**
     * Get the player instance for a post (if assigned).
     */
    fun getPlayerForPost(postId: String): ExoPlayer? {
        val index = playerAssignments.entries.find { it.value == postId }?.key
        return index?.let { players[it] }
    }
    
    /**
     * Check if a player is currently playing.
     */
    fun isPlaying(postId: String): Boolean {
        val player = getPlayerForPost(postId) ?: return false
        return player.isPlaying
    }
    
    /**
     * Get current playback position for a post.
     */
    fun getCurrentPosition(postId: String): Long {
        return getPlayerForPost(postId)?.currentPosition ?: 0L
    }
    
    /**
     * Get total duration for a post's video.
     */
    fun getDuration(postId: String): Long {
        return getPlayerForPost(postId)?.duration ?: 0L
    }
    
    /**
     * Release all players. Call when the fragment/activity is destroyed.
     */
    fun releaseAll() {
        try {
            playerAssignments.clear()
            activePlayers.clear()
            
            players.forEach { player ->
                try {
                    if (player.playbackState != Player.STATE_IDLE) {
                        player.stop()
                    }
                    player.clearMediaItems()
                    player.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing player: ${e.message}")
                }
            }
            Log.d(TAG, "Released all players")
        } catch (e: Exception) {
            Log.e(TAG, "Error in releaseAll: ${e.message}")
        }
    }
    
    /**
     * Add a listener to a player for a specific post.
     */
    fun addPlayerListener(postId: String, listener: Player.Listener) {
        getPlayerForPost(postId)?.addListener(listener)
    }
    
    /**
     * Remove a listener from a player for a specific post.
     */
    fun removePlayerListener(postId: String, listener: Player.Listener) {
        getPlayerForPost(postId)?.removeListener(listener)
    }
}
