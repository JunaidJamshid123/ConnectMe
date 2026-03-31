package com.junaidjamshid.i211203.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.repository.NotificationRepository
import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.domain.usecase.auth.GetCurrentUserUseCase
import com.junaidjamshid.i211203.domain.usecase.post.GetFeedPostsUseCase
import com.junaidjamshid.i211203.domain.usecase.post.LikePostUseCase
import com.junaidjamshid.i211203.domain.usecase.post.UnlikePostUseCase
import com.junaidjamshid.i211203.domain.usecase.story.GetStoriesUseCase
import com.junaidjamshid.i211203.util.NotificationEventBus
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen following Clean Architecture.
 * Supports image posts, carousels, and video/reel posts with view tracking.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val getStoriesUseCase: GetStoriesUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationEventBus: NotificationEventBus
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val currentUserId: String?
        get() = getCurrentUserUseCase.getCurrentUserId()
    
    init {
        loadCurrentUser()
        loadPosts()
        loadStories()
        loadFollowing()
        loadSuggestions()
        loadNotificationCount()
        subscribeToNotifications()
        subscribeToEventBus()
    }
    
    private fun loadNotificationCount() {
        viewModelScope.launch {
            when (val result = notificationRepository.getUnreadCount()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(unreadNotificationCount = result.data ?: 0) }
                }
                else -> { /* Ignore errors */ }
            }
        }
    }
    
    private fun subscribeToNotifications() {
        notificationRepository.subscribeToNewNotifications()
            .onEach { notification ->
                Log.d("HomeViewModel", "New notification received via realtime: ${notification.type}")
                // Increment unread count when new notification arrives
                _uiState.update { state ->
                    state.copy(unreadNotificationCount = state.unreadNotificationCount + 1)
                }
            }
            .catch { e ->
                Log.e("HomeViewModel", "Error in notification subscription: ${e.message}")
            }
            .launchIn(viewModelScope)
    }
    
    private fun subscribeToEventBus() {
        // Listen for new notifications from event bus (for local notifications created by current user's actions)
        notificationEventBus.newNotifications
            .onEach { notification ->
                Log.d("HomeViewModel", "New notification via event bus: ${notification.type}")
                _uiState.update { state ->
                    state.copy(unreadNotificationCount = state.unreadNotificationCount + 1)
                }
            }
            .launchIn(viewModelScope)
        
        // Listen for unread count changes
        notificationEventBus.unreadCountChanged
            .onEach { count ->
                _uiState.update { it.copy(unreadNotificationCount = count) }
            }
            .launchIn(viewModelScope)
        
        // Listen for all read event
        notificationEventBus.allRead
            .onEach {
                _uiState.update { it.copy(unreadNotificationCount = 0) }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Call this when returning from notifications screen to refresh count
     */
    fun refreshNotificationCount() {
        loadNotificationCount()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = getCurrentUserUseCase()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(currentUser = result.data) }
                }
                is Resource.Error -> { }
                is Resource.Loading -> { }
            }
        }
    }
    
    private fun loadPosts() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingPosts = true, postsError = null) }
                
                getFeedPostsUseCase(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update { 
                                it.copy(
                                    posts = result.data ?: emptyList(),
                                    isLoadingPosts = false,
                                    isRefreshing = false
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { 
                                it.copy(
                                    postsError = result.message,
                                    isLoadingPosts = false,
                                    isRefreshing = false
                                ) 
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoadingPosts = true) }
                        }
                    }
                }
            }
        }
    }
    
    private fun loadStories() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingStories = true, storiesError = null) }
                
                getStoriesUseCase(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val stories = result.data ?: emptyList()
                            // Check if current user has any active story
                            val currentUserHasStory = stories.any { it.userId == userId }
                            _uiState.update { 
                                it.copy(
                                    stories = stories,
                                    currentUserHasStory = currentUserHasStory,
                                    isLoadingStories = false
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { 
                                it.copy(
                                    storiesError = result.message,
                                    isLoadingStories = false
                                ) 
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoadingStories = true) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Load the list of user IDs the current user is following.
     */
    private fun loadFollowing() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                userRepository.getFollowing(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val followingIds = result.data?.map { it.userId }?.toSet() ?: emptySet()
                            _uiState.update { it.copy(followingUserIds = followingIds) }
                        }
                        is Resource.Error -> { }
                        is Resource.Loading -> { }
                    }
                }
            }
        }
    }

    /**
     * Load suggested users (all users minus current user, shuffled).
     */
    private fun loadSuggestions() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                userRepository.getAllUsers().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val suggestions = (result.data ?: emptyList())
                                .filter { it.userId != userId }
                                .shuffled()
                                .take(15) // Limit inline suggestions
                            _uiState.update { it.copy(suggestedUsers = suggestions) }
                        }
                        is Resource.Error -> { }
                        is Resource.Loading -> { }
                    }
                }
            }
        }
    }
    
    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadPosts()
        loadStories()
        loadFollowing()
        loadSuggestions()
    }
    
    /**
     * Like/unlike a post with reactive (optimistic) UI update.
     */
    fun onLikePost(postId: String) {
        currentUserId?.let { userId ->
            val post = _uiState.value.posts.find { it.postId == postId } ?: return@let
            val isCurrentlyLiked = post.isLikedByCurrentUser
            val newLikesCount = if (isCurrentlyLiked) post.likesCount - 1 else post.likesCount + 1

            // Optimistic update - immediately update UI
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { p ->
                        if (p.postId == postId) {
                            p.copy(
                                isLikedByCurrentUser = !isCurrentlyLiked,
                                likesCount = newLikesCount.coerceAtLeast(0)
                            )
                        } else p
                    }
                )
            }

            // Perform actual operation in background
            viewModelScope.launch {
                val result = if (isCurrentlyLiked) {
                    unlikePostUseCase(postId, userId)
                } else {
                    likePostUseCase(postId, userId)
                }
                
                // If operation failed, revert the optimistic update
                if (result is Resource.Error) {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { p ->
                                if (p.postId == postId) {
                                    p.copy(
                                        isLikedByCurrentUser = isCurrentlyLiked,
                                        likesCount = post.likesCount
                                    )
                                } else p
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Follow/unfollow a user with reactive (optimistic) UI update.
     */
    fun onFollowUser(targetUserId: String) {
        val userId = currentUserId ?: return
        val isCurrentlyFollowing = _uiState.value.followingUserIds.contains(targetUserId)
        
        // Optimistic update - immediately update UI
        _uiState.update {
            if (isCurrentlyFollowing) {
                it.copy(followingUserIds = it.followingUserIds - targetUserId)
            } else {
                it.copy(followingUserIds = it.followingUserIds + targetUserId)
            }
        }
        
        // Perform actual operation in background
        viewModelScope.launch {
            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId, targetUserId)
            } else {
                userRepository.followUser(userId, targetUserId)
            }
            
            // If operation failed, revert the optimistic update
            if (result is Resource.Error) {
                _uiState.update {
                    if (isCurrentlyFollowing) {
                        it.copy(followingUserIds = it.followingUserIds + targetUserId)
                    } else {
                        it.copy(followingUserIds = it.followingUserIds - targetUserId)
                    }
                }
            }
        }
    }
    
    fun onSavePost(postId: String) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                // Find current save state
                val currentPost = _uiState.value.posts.find { it.postId == postId }
                val isCurrentlySaved = currentPost?.isSavedByCurrentUser ?: false
                
                // Optimistic update
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.map { post ->
                            if (post.postId == postId) {
                                post.copy(isSavedByCurrentUser = !isCurrentlySaved)
                            } else post
                        }
                    )
                }
                
                // Make API call
                val result = if (isCurrentlySaved) {
                    postRepository.unsavePost(postId, userId)
                } else {
                    postRepository.savePost(postId, userId)
                }
                
                // Revert on failure
                if (result is Resource.Error) {
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.postId == postId) {
                                    post.copy(isSavedByCurrentUser = isCurrentlySaved)
                                } else post
                            },
                            postsError = result.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Track a video view after minimum watch time (3 seconds).
     * Updates the view count optimistically in UI.
     */
    fun trackVideoView(postId: String) {
        currentUserId?.let { userId ->
            // Optimistic update - increment view count locally
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.map { post ->
                        if (post.postId == postId && post.isVideo) {
                            post.copy(viewsCount = post.viewsCount + 1)
                        } else post
                    }
                )
            }
            
            // Record the view in the backend
            viewModelScope.launch {
                try {
                    postRepository.recordVideoView(postId, userId)
                    Log.d("HomeViewModel", "Video view recorded for post: $postId")
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Failed to record video view: ${e.message}")
                    // Note: We don't revert the UI update for view counts 
                    // as it's not critical and avoids flicker
                }
            }
        }
    }
}
