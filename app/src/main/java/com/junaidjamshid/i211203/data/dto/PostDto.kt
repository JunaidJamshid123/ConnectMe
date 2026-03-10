package com.junaidjamshid.i211203.data.dto

/**
 * Data Transfer Object for Post data.
 * Supports image posts, carousels, and video/reel posts.
 */
data class PostDto(
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
    val likes: MutableMap<String, Boolean> = mutableMapOf(),
    val comments: MutableList<CommentDto> = mutableListOf(),
    // Video/Reel fields
    val mediaType: String = "image",      // "image", "video", "reel"
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val videoDuration: Int = 0,           // Duration in milliseconds
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val aspectRatio: Float = 1f,
    val viewsCount: Int = 0
)

/**
 * Data Transfer Object for Comment data from Firebase.
 */
data class CommentDto(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val likes: MutableMap<String, Boolean> = mutableMapOf()
)
