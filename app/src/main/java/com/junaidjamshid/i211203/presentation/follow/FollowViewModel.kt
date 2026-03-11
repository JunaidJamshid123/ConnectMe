package com.junaidjamshid.i211203.presentation.follow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.repository.AuthRepository
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
 * ViewModel for Followers/Following screens.
 */
@HiltViewModel
class FollowViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FollowUiState())
    val uiState: StateFlow<FollowUiState> = _uiState.asStateFlow()
    
    private val currentUserId: String?
        get() = authRepository.getCurrentUserId()
    
    private var targetUserId: String? = null
    
    /**
     * Get current user ID for adapter to prevent self-follow
     */
    fun getLoggedInUserId(): String? = currentUserId
    
    /**
     * Initialize with user ID and load both counts + followers list
     */
    fun initialize(userId: String?, initialTab: FollowTab = FollowTab.FOLLOWERS) {
        targetUserId = userId ?: currentUserId
        targetUserId?.let { id ->
            loadCounts(id)
            _uiState.update { it.copy(currentTab = initialTab) }
            when (initialTab) {
                FollowTab.FOLLOWERS -> loadFollowers(id)
                FollowTab.FOLLOWING -> loadFollowing(id)
            }
        }
    }
    
    /**
     * Load follower and following counts
     */
    private fun loadCounts(userId: String) {
        viewModelScope.launch {
            userRepository.getFollowers(userId).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(followersCount = result.data?.size ?: 0) }
                }
            }
        }
        viewModelScope.launch {
            userRepository.getFollowing(userId).collect { result ->
                if (result is Resource.Success) {
                    _uiState.update { it.copy(followingCount = result.data?.size ?: 0) }
                }
            }
        }
    }
    
    /**
     * Switch to a tab
     */
    fun switchTab(tab: FollowTab) {
        if (_uiState.value.currentTab == tab) return
        _uiState.update { it.copy(currentTab = tab) }
        targetUserId?.let { id ->
            when (tab) {
                FollowTab.FOLLOWERS -> loadFollowers(id)
                FollowTab.FOLLOWING -> loadFollowing(id)
            }
        }
    }
    
    /**
     * Load followers for a user
     */
    fun loadFollowers(userId: String? = null) {
        val targetId = userId ?: targetUserId ?: currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            userRepository.getFollowers(targetId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val users = result.data ?: emptyList()
                        val followUsers = users.map { user ->
                            FollowUser(
                                user = user,
                                isFollowing = checkIfFollowing(user.userId),
                                isFollowedBy = true
                            )
                        }
                        
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                users = followUsers,
                                followersCount = users.size,
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
     * Load following for a user
     */
    fun loadFollowing(userId: String? = null) {
        val targetId = userId ?: targetUserId ?: currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            userRepository.getFollowing(targetId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val users = result.data ?: emptyList()
                        val followUsers = users.map { user ->
                            FollowUser(
                                user = user,
                                isFollowing = true,
                                isFollowedBy = checkIfFollowedBy(user.userId)
                            )
                        }
                        
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                users = followUsers,
                                followingCount = users.size,
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
     * Toggle follow status for a user with optimistic update
     */
    fun toggleFollow(userId: String) {
        // Prevent self-follow
        if (userId == currentUserId) return
        
        val userIndex = _uiState.value.users.indexOfFirst { it.user.userId == userId }
        if (userIndex == -1) return
        
        val followUser = _uiState.value.users[userIndex]
        val isCurrentlyFollowing = followUser.isFollowing
        
        // Optimistic update
        _uiState.update { state ->
            val updatedUsers = state.users.toMutableList()
            updatedUsers[userIndex] = followUser.copy(isFollowing = !isCurrentlyFollowing)
            state.copy(
                users = updatedUsers,
                followingCount = if (isCurrentlyFollowing) 
                    (state.followingCount - 1).coerceAtLeast(0)
                else 
                    state.followingCount + 1
            )
        }
        
        viewModelScope.launch {
            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }
            
            // Revert on error
            if (result is Resource.Error) {
                _uiState.update { state ->
                    val updatedUsers = state.users.toMutableList()
                    if (userIndex < updatedUsers.size) {
                        updatedUsers[userIndex] = followUser
                    }
                    state.copy(
                        users = updatedUsers,
                        followingCount = if (isCurrentlyFollowing)
                            state.followingCount + 1
                        else
                            (state.followingCount - 1).coerceAtLeast(0),
                        error = result.message
                    )
                }
            }
        }
    }
    
    /**
     * Remove a follower with optimistic update
     */
    fun removeFollower(userId: String) {
        val removedUser = _uiState.value.users.find { it.user.userId == userId }
        
        // Optimistic update
        _uiState.update { state ->
            state.copy(
                users = state.users.filter { it.user.userId != userId },
                followersCount = (state.followersCount - 1).coerceAtLeast(0)
            )
        }
        
        viewModelScope.launch {
            val result = userRepository.removeFollower(userId)
            
            // Revert on error
            if (result is Resource.Error && removedUser != null) {
                _uiState.update { state ->
                    state.copy(
                        users = state.users + removedUser,
                        followersCount = state.followersCount + 1,
                        error = result.message
                    )
                }
            }
        }
    }
    
    private suspend fun checkIfFollowing(userId: String): Boolean {
        val currentId = currentUserId ?: return false
        var isFollowing = false
        
        userRepository.isFollowing(currentId, userId).collect { result ->
            if (result is Resource.Success) {
                isFollowing = result.data ?: false
            }
        }
        
        return isFollowing
    }
    
    private suspend fun checkIfFollowedBy(userId: String): Boolean {
        val currentId = currentUserId ?: return false
        var isFollowedBy = false
        
        userRepository.isFollowing(userId, currentId).collect { result ->
            if (result is Resource.Success) {
                isFollowedBy = result.data ?: false
            }
        }
        
        return isFollowedBy
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
