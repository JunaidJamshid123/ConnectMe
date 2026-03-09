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
 * UI State for Add Post screen — supports multi-image, location, and music.
 */
data class AddPostUiState(
    val isLoading: Boolean = false,
    val selectedImageUri: String? = null,
    val caption: String = "",
    val postCreated: Boolean = false,
    val error: String? = null,
    // Multi-image support
    val selectedImageBytesList: List<ByteArray> = emptyList(),
    val currentPreviewIndex: Int = 0,
    // Location
    val location: String = "",
    // Music
    val musicName: String = "",
    val musicArtist: String = "",
    // Convenience
    val hasImages: Boolean = false
)
