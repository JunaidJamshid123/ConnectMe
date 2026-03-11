package com.junaidjamshid.i211203.presentation.reels

import com.junaidjamshid.i211203.domain.model.Post

/**
 * UI State for Reels screen.
 */
data class ReelsUiState(
    val isLoading: Boolean = false,
    val reels: List<Post> = emptyList(),
    val currentPosition: Int = 0,
    val error: String? = null
)
