package com.junaidjamshid.i211203.models

data class User(
    val userId: String = "",  // Unique Firebase UID
    val username: String = "",  // Unique username
    val email: String = "",
    val fullName: String = "",  // Full name
    val phoneNumber: String = "",  // Phone number (default empty)
    val profilePictureUrl: String = "",  // Profile picture URL (Firebase Storage)
    val coverPhotoUrl: String = "",  // Cover photo URL
    val bio: String = "",  // User bio
    val followers: MutableList<String> = mutableListOf(),  // ✅ Ensures empty list is stored
    val following: MutableList<String> = mutableListOf(),  // ✅ Ensures empty list is stored
    val blockedUsers: MutableList<String> = mutableListOf(),  // ✅ Ensures empty list is stored
    val onlineStatus: Boolean = false,
    val pushToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val vanishModeEnabled: Boolean = false,
    val storyExpiryTimestamp: Long? = null  // ✅ Ensures nullable field is stored
)
