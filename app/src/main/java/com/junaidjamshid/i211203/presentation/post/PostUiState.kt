package com.junaidjamshid.i211203.presentation.post

import com.junaidjamshid.i211203.domain.model.Comment
import com.junaidjamshid.i211203.domain.model.Post

/**
 * UI State for Post-related screens.
 */
data class PostUiState(
    val isLoading: Boolean = false,
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val postCreated: Boolean = false,
    val error: String? = null
)

/**
 * UI State for Add Post screen.
 */
data class AddPostUiState(
    val isLoading: Boolean = false,
    val selectedImageUri: String? = null,
    val caption: String = "",
    val postCreated: Boolean = false,
    val error: String? = null
)
