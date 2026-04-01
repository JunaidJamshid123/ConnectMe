package com.junaidjamshid.i211203.presentation.profile

import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.model.StoryHighlight
import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Profile screens.
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val posts: List<Post> = emptyList(),           // All posts (for backward compatibility)
    val imagePosts: List<Post> = emptyList(),      // Only image posts (no videos)
    val reelPosts: List<Post> = emptyList(),       // Only video/reel posts
    val savedPosts: List<Post> = emptyList(),      // Saved posts
    val highlights: List<StoryHighlight> = emptyList(), // Story highlights
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false,
    val isCurrentUser: Boolean = false,
    val hasActiveStories: Boolean = false,
    val error: String? = null,
    val logoutSuccess: Boolean = false,
    val profileUpdateSuccess: Boolean = false
)
