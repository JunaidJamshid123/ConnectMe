package com.junaidjamshid.i211203.models

class User {
    var userId: String = ""
    var username: String = ""
    var email: String = ""
    var fullName: String = ""
    var phoneNumber: String = ""
    var profilePictureUrl: String = ""
    var coverPhotoUrl: String = ""
    var bio: String = ""
    var followers: HashMap<String, Any> = hashMapOf()
    var following: HashMap<String, Any> = hashMapOf()
    var blockedUsers: HashMap<String, Any> = hashMapOf()
    var onlineStatus: Boolean = false
    var pushToken: String = ""
    var createdAt: Long = System.currentTimeMillis()
    var lastSeen: Long = System.currentTimeMillis()
    var vanishModeEnabled: Boolean = false
    var storyExpiryTimestamp: Long? = null


    constructor()


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