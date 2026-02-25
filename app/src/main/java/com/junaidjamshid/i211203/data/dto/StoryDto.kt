package com.junaidjamshid.i211203.data.dto

/**
 * Data Transfer Object for Story data from Firebase.
 */
data class StoryDto(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val storyImageUrl: String = "",
    val timestamp: Long = 0,
    val expiryTimestamp: Long = 0,
    val viewers: MutableMap<String, Boolean> = mutableMapOf()
)
