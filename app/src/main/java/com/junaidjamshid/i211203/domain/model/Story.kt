package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Story in the application.
 */
data class Story(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val storyImageUrl: String = "",
    val timestamp: Long = 0,
    val expiryTimestamp: Long = 0,
    val viewersCount: Int = 0,
    val isViewedByCurrentUser: Boolean = false
)
