package com.junaidjamshid.i211203.models

class User {
    var userId: String = ""  // Unique Firebase UID
    var username: String = ""  // Unique username
    var email: String = ""
    var fullName: String = ""  // Full name
    var phoneNumber: String = ""  // Phone number (default empty)
    var profilePictureUrl: String = ""  // Profile picture URL (Firebase Storage)
    var coverPhotoUrl: String = ""  // Cover photo URL
    var bio: String = ""  // User bio
    var followers: HashMap<String, Any> = hashMapOf()  // ✅ Store as a HashMap
    var following: HashMap<String, Any> = hashMapOf()
    var blockedUsers: HashMap<String, Any> = hashMapOf()
    var onlineStatus: Boolean = false
    var pushToken: String = ""
    var createdAt: Long = System.currentTimeMillis()
    var lastSeen: Long = System.currentTimeMillis()
    var vanishModeEnabled: Boolean = false
    var storyExpiryTimestamp: Long? = null  // Ensures nullable field is stored

    // Default constructor required for Firebase
    constructor()

    // Constructor with parameters
    constructor(
        userId: String,
        username: String,
        email: String,
        fullName: String,
        phoneNumber: String,
        profilePictureUrl: String,
        coverPhotoUrl: String,
        bio: String,
        followers: HashMap<String, Any>,
        following: HashMap<String, Any>,
        blockedUsers: HashMap<String, Any>,
        onlineStatus: Boolean,
        pushToken: String,
        createdAt: Long,
        lastSeen: Long,
        vanishModeEnabled: Boolean,
        storyExpiryTimestamp: Long?
    ) {
        this.userId = userId
        this.username = username
        this.email = email
        this.fullName = fullName
        this.phoneNumber = phoneNumber
        this.profilePictureUrl = profilePictureUrl
        this.coverPhotoUrl = coverPhotoUrl
        this.bio = bio
        this.followers = followers
        this.following = following
        this.blockedUsers = blockedUsers
        this.onlineStatus = onlineStatus
        this.pushToken = pushToken
        this.createdAt = createdAt
        this.lastSeen = lastSeen
        this.vanishModeEnabled = vanishModeEnabled
        this.storyExpiryTimestamp = storyExpiryTimestamp
    }
}