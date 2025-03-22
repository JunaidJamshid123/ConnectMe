package com.junaidjamshid.i211203.models

import android.util.Base64

class User {
    var userId: String = ""
    var username: String = ""
    var email: String = ""
    var fullName: String = ""
    var phoneNumber: String = ""
    // Changed from ByteArray to String to store Base64-encoded image
    var profilePicture: String? = null
    var coverPhoto: String? = null
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
        profilePicture: ByteArray?,
        coverPhoto: ByteArray?,
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
        // Convert ByteArray to Base64 String if not null
        this.profilePicture = profilePicture?.let { Base64.encodeToString(it, Base64.DEFAULT) }
        this.coverPhoto = coverPhoto?.let { Base64.encodeToString(it, Base64.DEFAULT) }
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

    // Helper method to get profile picture as ByteArray
    fun getProfilePictureBytes(): ByteArray? {
        return profilePicture?.let { Base64.decode(it, Base64.DEFAULT) }
    }

    // Helper method to get cover photo as ByteArray
    fun getCoverPhotoBytes(): ByteArray? {
        return coverPhoto?.let { Base64.decode(it, Base64.DEFAULT) }
    }

    // Helper method to set profile picture from ByteArray
    fun setProfilePictureFromBytes(bytes: ByteArray?) {
        profilePicture = bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) }
    }

    // Helper method to set cover photo from ByteArray
    fun setCoverPhotoFromBytes(bytes: ByteArray?) {
        coverPhoto = bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false

        if (userId != other.userId) return false
        if (profilePicture != other.profilePicture) return false
        if (coverPhoto != other.coverPhoto) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + (profilePicture?.hashCode() ?: 0)
        result = 31 * result + (coverPhoto?.hashCode() ?: 0)
        return result
    }
}