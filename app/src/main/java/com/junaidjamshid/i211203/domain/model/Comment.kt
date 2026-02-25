package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Comment on a Post.
 */
data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val isLikedByCurrentUser: Boolean = false
)
