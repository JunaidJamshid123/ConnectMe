package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a User in the application.
 * This is a pure Kotlin class with no framework dependencies.
 */
data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profilePicture: String? = null,
    val coverPhoto: String? = null,
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isOnline: Boolean = false,
    val pushToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val vanishModeEnabled: Boolean = false
)
