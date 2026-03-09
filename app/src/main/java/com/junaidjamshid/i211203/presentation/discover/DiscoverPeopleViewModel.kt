package com.junaidjamshid.i211203.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.domain.usecase.auth.GetCurrentUserUseCase
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Discover People screen.
 */
data class DiscoverPeopleUiState(
    val suggestions: List<User> = emptyList(),
    val followingUserIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dismissedUserIds: Set<String> = emptySet()
)

/**
 * ViewModel for the Discover People screen.
 * Loads all users, filters out current user + already following, and handles follow/unfollow.
 */
@HiltViewModel
class DiscoverPeopleViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverPeopleUiState())
    val uiState: StateFlow<DiscoverPeopleUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = getCurrentUserUseCase.getCurrentUserId()

    init {
        loadSuggestions()
    }

    fun loadSuggestions() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // First, get who current user is following
            launch {
                userRepository.getFollowing(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val followingIds = result.data?.map { it.userId }?.toSet() ?: emptySet()
                            _uiState.update { it.copy(followingUserIds = followingIds) }
                        }
                        is Resource.Error -> { /* ignore */ }
                        is Resource.Loading -> { /* ignore */ }
                    }
                }
            }

            // Then load all users
            launch {
                userRepository.getAllUsers().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val allUsers = result.data ?: emptyList()
                            // Filter out current user
                            val filtered = allUsers.filter { it.userId != userId }
                            _uiState.update {
                                it.copy(
                                    suggestions = filtered,
                                    isLoading = false
                                )
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update {
                                it.copy(
                                    error = result.message,
                                    isLoading = false
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            }
        }
    }

    fun onFollowClick(targetUserId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val isFollowing = _uiState.value.followingUserIds.contains(targetUserId)
            if (isFollowing) {
                // Unfollow
                val result = userRepository.unfollowUser(userId, targetUserId)
                if (result is Resource.Success) {
                    _uiState.update {
                        it.copy(followingUserIds = it.followingUserIds - targetUserId)
                    }
                }
            } else {
                // Follow
                val result = userRepository.followUser(userId, targetUserId)
                if (result is Resource.Success) {
                    _uiState.update {
                        it.copy(followingUserIds = it.followingUserIds + targetUserId)
                    }
                }
            }
        }
    }

    fun onDismiss(userId: String) {
        _uiState.update {
            it.copy(dismissedUserIds = it.dismissedUserIds + userId)
        }
    }

    /** Get visible suggestions (not dismissed) */
    fun getVisibleSuggestions(): List<User> {
        val state = _uiState.value
        return state.suggestions.filter { it.userId !in state.dismissedUserIds }
    }
}
