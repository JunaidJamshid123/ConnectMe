package com.junaidjamshid.i211203.presentation.follow

import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Followers/Following screens.
 */
data class FollowUiState(
    val isLoading: Boolean = false,
    val users: List<FollowUser> = emptyList(),
    val error: String? = null
)

/**
 * Represents a user in followers/following list with follow status.
 */
data class FollowUser(
    val user: User,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false
)
