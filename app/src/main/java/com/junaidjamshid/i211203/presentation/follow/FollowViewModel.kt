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
    
    /**
     * Load followers for a user
     */
    fun loadFollowers(userId: String? = null) {
        val targetUserId = userId ?: currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            userRepository.getFollowers(targetUserId).collect { result ->
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
        val targetUserId = userId ?: currentUserId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            userRepository.getFollowing(targetUserId).collect { result ->
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
     * Toggle follow status for a user
     */
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            val userIndex = _uiState.value.users.indexOfFirst { it.user.userId == userId }
            if (userIndex == -1) return@launch
            
            val followUser = _uiState.value.users[userIndex]
            val isCurrentlyFollowing = followUser.isFollowing
            
            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        val updatedUsers = state.users.toMutableList()
                        updatedUsers[userIndex] = followUser.copy(isFollowing = !isCurrentlyFollowing)
                        state.copy(users = updatedUsers)
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Remove a follower
     */
    fun removeFollower(userId: String) {
        viewModelScope.launch {
            val result = userRepository.removeFollower(userId)
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(users = state.users.filter { it.user.userId != userId })
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> { /* Loading state */ }
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
