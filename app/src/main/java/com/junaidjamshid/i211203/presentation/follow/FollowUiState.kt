package com.junaidjamshid.i211203.presentation.follow

import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Followers/Following screens.
 */
data class FollowUiState(
    val isLoading: Boolean = false,
    val users: List<FollowUser> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val currentTab: FollowTab = FollowTab.FOLLOWERS,
    val error: String? = null
)

/**
 * Tab selection for followers/following screen.
 */
enum class FollowTab {
    FOLLOWERS, FOLLOWING
}

/**
 * Represents a user in followers/following list with follow status.
 */
data class FollowUser(
    val user: User,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false
)
