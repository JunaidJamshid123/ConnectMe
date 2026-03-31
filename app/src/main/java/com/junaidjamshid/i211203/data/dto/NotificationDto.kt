package com.junaidjamshid.i211203.data.dto

/**
 * Data Transfer Object for Notification data from Supabase.
 */
data class NotificationDto(
    val notificationId: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val actorProfileImage: String = "",
    val type: String = "",
    val postId: String? = null,
    val postThumbnail: String? = null,
    val storyId: String? = null,
    val commentId: String? = null,
    val commentText: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = 0
)
