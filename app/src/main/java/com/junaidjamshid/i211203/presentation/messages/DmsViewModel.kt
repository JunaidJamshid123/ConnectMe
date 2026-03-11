package com.junaidjamshid.i211203.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.User
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
 * ViewModel for DMs Activity.
 * Loads followed users (friends) for messaging.
 */
@HiltViewModel
class DmsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DmsUiState())
    val uiState: StateFlow<DmsUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = authRepository.getCurrentUserId()

    init {
        loadCurrentUser()
        loadFriends()
    }

    /**
     * Load current user's profile
     */
    private fun loadCurrentUser() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            when (val result = userRepository.getUserById(userId)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(currentUser = result.data) }
                }
                else -> { /* Ignore errors for current user */ }
            }
        }
    }

    /**
     * Load all followed users (friends)
     */
    fun loadFriends() {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Get users that current user is following
            userRepository.getFollowing(userId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val followedUsers = result.data ?: emptyList()
                        val friendItems = followedUsers.map { user ->
                            DmsFriendItem(
                                user = user,
                                lastMessage = "",
                                lastMessageTime = 0L,
                                isOnline = user.isOnline,
                                unreadCount = 0
                            )
                        }

                        // Sort by online status first
                        val sortedFriends = friendItems.sortedByDescending { it.isOnline }

                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                friends = sortedFriends,
                                filteredFriends = sortedFriends,
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
     * Filter friends by search query
     */
    fun searchFriends(query: String) {
        val trimmedQuery = query.trim().lowercase()
        
        if (trimmedQuery.isEmpty()) {
            _uiState.update { it.copy(filteredFriends = it.friends) }
            return
        }

        val filtered = _uiState.value.friends.filter { friend ->
            friend.user.username.lowercase().contains(trimmedQuery) ||
            friend.user.fullName.lowercase().contains(trimmedQuery)
        }
        
        _uiState.update { it.copy(filteredFriends = filtered) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
