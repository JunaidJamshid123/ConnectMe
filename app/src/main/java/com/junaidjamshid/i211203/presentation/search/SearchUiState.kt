package com.junaidjamshid.i211203.presentation.search

import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Search/Explore screens.
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val isLoadingExplore: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val recentSearches: List<User> = emptyList(),
    val allUsers: List<User> = emptyList(),
    val explorePosts: List<Post> = emptyList(),
    val error: String? = null
)
