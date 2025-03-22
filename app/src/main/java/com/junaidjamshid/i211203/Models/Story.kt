package com.junaidjamshid.i211203.models

class Story {
    var storyId: String = ""         // Unique ID for the story
    var userId: String = ""          // ID of the user who posted the story
    var username: String = ""        // Username of the user
    var userProfileImage: String = "" // Profile image URL of the user
    var storyImageUrl: String = ""   // URL of the uploaded story image (if any)
    var caption: String = ""         // Caption for the story (optional)
    var timestamp: Long = 0          // Time when the story was created (Unix timestamp)
    var expiryTimestamp: Long = 0    // Time when the story expires (Unix timestamp, 24 hours after creation)
    var viewers: MutableList<String> = mutableListOf() // List of user IDs who viewed the story

    constructor()

    constructor(
        storyId: String,
        userId: String,
        username: String,
        userProfileImage: String,
        storyImageUrl: String,
        caption: String,
        timestamp: Long,
        expiryTimestamp: Long,
        viewers: MutableList<String>
    ) {
        this.storyId = storyId
        this.userId = userId
        this.username = username
        this.userProfileImage = userProfileImage
        this.storyImageUrl = storyImageUrl
        this.caption = caption
        this.timestamp = timestamp
        this.expiryTimestamp = expiryTimestamp
        this.viewers = viewers
    }
}
