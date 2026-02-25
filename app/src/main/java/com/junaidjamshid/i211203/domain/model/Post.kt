package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Post in the application.
 */
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val postImageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false,
    val isSavedByCurrentUser: Boolean = false
)
