package com.junaidjamshid.i211203.models

class Post {
    var postId: String = ""          // Unique ID for the post
    var userId: String = ""          // ID of the user who created the post
    var username: String = ""        // Username of the creator
    var userProfileImage: String = ""// Profile image URL of the user
    var postImageUrl: String = ""    // URL of the uploaded image (if any)
    var caption: String = ""         // Post description or caption
    var timestamp: Long = 0          // Time when the post was created (Unix timestamp)
    var likes: MutableMap<String, Boolean> = mutableMapOf() // Map of userId to like status
    var comments: MutableList<Comment> = mutableListOf()    // List of comments

    constructor()

    constructor(
        postId: String,
        userId: String,
        username: String,
        userProfileImage: String,
        postImageUrl: String,
        caption: String,
        timestamp: Long,
        likes: MutableMap<String, Boolean>,
        comments: MutableList<Comment>
    ) {
        this.postId = postId
        this.userId = userId
        this.username = username
        this.userProfileImage = userProfileImage
        this.postImageUrl = postImageUrl
        this.caption = caption
        this.timestamp = timestamp
        this.likes = likes
        this.comments = comments
    }
}
