package com.junaidjamshid.i211203.models

class Comment {
    var commentId: String = ""      // Unique ID for the comment
    var postId: String = ""         // ID of the post the comment belongs to
    var userId: String = ""         // ID of the user who made the comment
    var username: String = ""       // Username of the commenter
    var userProfileImage: String = "" // Profile image URL of the commenter
    var text: String = ""           // Comment text
    var timestamp: Long = 0         // Time when the comment was created (Unix timestamp)
    var likes: MutableMap<String, Boolean> = mutableMapOf() // Map of userId to like status

    constructor()

    constructor(
        commentId: String,
        postId: String,
        userId: String,
        username: String,
        userProfileImage: String,
        text: String,
        timestamp: Long,
        likes: MutableMap<String, Boolean>
    ) {
        this.commentId = commentId
        this.postId = postId
        this.userId = userId
        this.username = username
        this.userProfileImage = userProfileImage
        this.text = text
        this.timestamp = timestamp
        this.likes = likes
    }
}
