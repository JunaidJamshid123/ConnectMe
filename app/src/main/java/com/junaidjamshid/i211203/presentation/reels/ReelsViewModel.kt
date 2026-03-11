package com.junaidjamshid.i211203.presentation.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.repository.AuthRepository
import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Reels screen - Instagram-style full-screen vertical video feed.
 */
@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = authRepository.getCurrentUserId()

    init {
        loadReels()
    }

    /**
     * Load all video posts (reels) from the feed.
     * Filters only posts where isVideo = true.
     */
    fun loadReels() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            postRepository.getFeedPosts(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        // Filter only video posts and shuffle for variety
                        val reels = result.data
                            ?.filter { it.isVideo && it.videoUrl.isNotBlank() }
                            ?.shuffled()
                            ?: emptyList()

                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                reels = reels,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * Update current reel position for tracking.
     */
    fun setCurrentPosition(position: Int) {
        _uiState.update { it.copy(currentPosition = position) }
    }

    /**
     * Like a reel.
     */
    fun likeReel(postId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            postRepository.likePost(postId, userId)
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    reels = state.reels.map { reel ->
                        if (reel.postId == postId) {
                            reel.copy(
                                isLikedByCurrentUser = true,
                                likesCount = reel.likesCount + 1
                            )
                        } else reel
                    }
                )
            }
        }
    }

    /**
     * Unlike a reel.
     */
    fun unlikeReel(postId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            postRepository.unlikePost(postId, userId)
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    reels = state.reels.map { reel ->
                        if (reel.postId == postId) {
                            reel.copy(
                                isLikedByCurrentUser = false,
                                likesCount = (reel.likesCount - 1).coerceAtLeast(0)
                            )
                        } else reel
                    }
                )
            }
        }
    }

    /**
     * Save a reel.
     */
    fun saveReel(postId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            postRepository.savePost(postId, userId)
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    reels = state.reels.map { reel ->
                        if (reel.postId == postId) {
                            reel.copy(isSavedByCurrentUser = true)
                        } else reel
                    }
                )
            }
        }
    }

    /**
     * Unsave a reel.
     */
    fun unsaveReel(postId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            postRepository.unsavePost(postId, userId)
            
            // Optimistic update
            _uiState.update { state ->
                state.copy(
                    reels = state.reels.map { reel ->
                        if (reel.postId == postId) {
                            reel.copy(isSavedByCurrentUser = false)
                        } else reel
                    }
                )
            }
        }
    }

    /**
     * Record that the current user viewed a video.
     */
    fun recordVideoView(postId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            postRepository.recordVideoView(postId, userId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
