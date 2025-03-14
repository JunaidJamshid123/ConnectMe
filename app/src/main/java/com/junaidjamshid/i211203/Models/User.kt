package com.junaidjamshid.i211203.Models

data class User(
    val userId: String = "",  // Unique Firebase UID
    var username: String = "",  // Unique username
    var email: String = "",
    var fullName: String = "",  // Full name
    var phoneNumber: String? = null,  // Optional phone number
    var profilePictureUrl: String = "",  // Profile picture URL (Firebase Storage)
    var coverPhotoUrl: String = "",  // Cover photo URL
    var bio: String = "",  // Short user bio
    var followers: MutableList<String> = mutableListOf(),  // List of user IDs who follow this user
    var following: MutableList<String> = mutableListOf(),  // List of user IDs this user follows
    var blockedUsers: MutableList<String> = mutableListOf(),  // List of blocked users
    var onlineStatus: Boolean = false,  // True = online, False = offline
    var pushToken: String = "",  // Firebase Cloud Messaging (FCM) token
    val createdAt: Long = System.currentTimeMillis(),  // Account creation timestamp
    var lastSeen: Long = System.currentTimeMillis(),  // Last active timestamp
    var vanishModeEnabled: Boolean = false,  // Vanish mode for messages
    var storyExpiryTimestamp: Long? = null // Timestamp for the latest story expiry
)
