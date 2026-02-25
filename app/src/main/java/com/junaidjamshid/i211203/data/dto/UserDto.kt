package com.junaidjamshid.i211203.data.dto

/**
 * Data Transfer Object for User data from Firebase.
 */
data class UserDto(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profilePicture: String? = null,
    val coverPhoto: String? = null,
    val bio: String = "",
    val followers: HashMap<String, Any> = hashMapOf(),
    val following: HashMap<String, Any> = hashMapOf(),
    val blockedUsers: HashMap<String, Any> = hashMapOf(),
    val onlineStatus: Boolean = false,
    val pushToken: String = "",
    val createdAt: Long = 0,
    val lastSeen: Long = 0,
    val vanishModeEnabled: Boolean = false
)
