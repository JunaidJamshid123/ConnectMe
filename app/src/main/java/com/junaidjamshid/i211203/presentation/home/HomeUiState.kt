package com.junaidjamshid.i211203.presentation.home

import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.model.Story
import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Home screen.
 */
data class HomeUiState(
    val currentUser: User? = null,
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
    val isLoadingPosts: Boolean = false,
    val isLoadingStories: Boolean = false,
    val postsError: String? = null,
    val storiesError: String? = null,
    val isRefreshing: Boolean = false
)
