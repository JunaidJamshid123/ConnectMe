package com.junaidjamshid.i211203.presentation.profile

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
 * ViewModel for Profile screens (ProfileFragment, UserProfile).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val currentUserId: String?
        get() = authRepository.getCurrentUserId()
    
    /**
     * Load current user's profile
     */
    fun loadCurrentUserProfile() {
        val userId = currentUserId ?: return
        loadProfile(userId, isCurrentUser = true)
    }
    
    /**
     * Load another user's profile
     */
    fun loadUserProfile(userId: String) {
        val isCurrentUser = userId == currentUserId
        loadProfile(userId, isCurrentUser)
    }
    
    private fun loadProfile(userId: String, isCurrentUser: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isCurrentUser = isCurrentUser) }
            
            // Load user data
            userRepository.getUserProfile(userId).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        result.data?.let { user ->
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    user = user,
                                    followersCount = user.followersCount,
                                    followingCount = user.followingCount,
                                    error = null
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { 
                            it.copy(isLoading = false, error = result.message) 
                        }
                    }
                }
            }
            
            // Load user posts
            loadUserPosts(userId)
            
            // Check follow status if viewing another user
            if (!isCurrentUser) {
                checkFollowStatus(userId)
            }
        }
    }
    
    private fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            postRepository.getUserPosts(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { posts ->
                            _uiState.update { state ->
                                state.copy(
                                    posts = posts,
                                    postsCount = posts.size
                                )
                            }
                        }
                    }
                    else -> { /* Handle other states */ }
                }
            }
        }
    }
    
    private fun checkFollowStatus(userId: String) {
        viewModelScope.launch {
            val currentId = currentUserId ?: return@launch
            userRepository.isFollowing(currentId, userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isFollowing = result.data ?: false) }
                    }
                    else -> { /* Handle other states */ }
                }
            }
        }
    }
    
    /**
     * Toggle follow status for a user
     */
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            val isCurrentlyFollowing = _uiState.value.isFollowing
            
            val result = if (isCurrentlyFollowing) {
                userRepository.unfollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isFollowing = !isCurrentlyFollowing,
                            followersCount = if (isCurrentlyFollowing) 
                                state.followersCount - 1 
                            else 
                                state.followersCount + 1
                        )
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
     * Update user profile
     */
    fun updateProfile(
        fullName: String,
        username: String,
        phone: String,
        bio: String,
        profileImageBytes: ByteArray?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = userRepository.updateUserProfile(
                fullName = fullName,
                username = username,
                phone = phone,
                bio = bio,
                profileImage = profileImageBytes
            )
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            profileUpdateSuccess = true,
                            error = null
                        ) 
                    }
                }
                is Resource.Error -> {
                    _uiState.update { 
                        it.copy(isLoading = false, error = result.message) 
                    }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        viewModelScope.launch {
            val result = authRepository.logout()
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(logoutSuccess = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> { /* Loading state */ }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearProfileUpdateSuccess() {
        _uiState.update { it.copy(profileUpdateSuccess = false) }
    }
}
