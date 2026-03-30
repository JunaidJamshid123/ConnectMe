package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Notification in the application.
 * Supports Instagram-like notification types.
 */
data class Notification(
    val notificationId: String = "",
    val userId: String = "",           // User who performed the action
    val username: String = "",
    val userProfileImage: String = "",
    val targetUserId: String = "",     // User receiving the notification
    val type: NotificationType = NotificationType.LIKE,
    val postId: String? = null,
    val postThumbnail: String? = null,
    val commentText: String? = null,
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val isFollowing: Boolean = false,  // Whether target user follows back
    val hasStory: Boolean = false      // Whether the user has active story
) {
    /**
     * Get the time ago string for display
     */
    val timeAgo: String
        get() {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7
            
            return when {
                seconds < 60 -> "now"
                minutes < 60 -> "${minutes}m"
                hours < 24 -> "${hours}h"
                days < 7 -> "${days}d"
                else -> "${weeks}w"
            }
        }
}

/**
 * Types of notifications
 */
enum class NotificationType {
    LIKE,           // Someone liked your post
    COMMENT,        // Someone commented on your post
    FOLLOW,         // Someone started following you
    FOLLOW_REQUEST, // Someone requested to follow you
    MENTION,        // Someone mentioned you
    LIKE_COMMENT,   // Someone liked your comment
    TAG             // Someone tagged you in a post
}

/**
 * Section categories for grouping notifications
 */
enum class NotificationSection {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    EARLIER
}
