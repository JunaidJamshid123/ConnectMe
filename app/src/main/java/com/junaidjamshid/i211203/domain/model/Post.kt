package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Post in the application.
 * Supports Instagram-like features: multiple images (carousel), location, music.
 */
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val caption: String = "",
    val location: String = "",
    val musicName: String = "",
    val musicArtist: String = "",
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val isSavedByCurrentUser: Boolean = false
) {
    /** Whether this post is a carousel (multiple images) */
    val isCarousel: Boolean get() = imageUrls.size > 1
    
    /** Whether this post has music attached */
    val hasMusic: Boolean get() = musicName.isNotBlank()
    
    /** Whether this post has a location tagged */
    val hasLocation: Boolean get() = location.isNotBlank()
    
    /** Get all displayable images: imageUrls if present, else fallback to single postImageUrl */
    val allImages: List<String>
        get() = imageUrls.ifEmpty { if (postImageUrl.isNotEmpty()) listOf(postImageUrl) else emptyList() }
}
