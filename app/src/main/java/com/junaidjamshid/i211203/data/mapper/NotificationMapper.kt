package com.junaidjamshid.i211203.data.mapper

import com.junaidjamshid.i211203.data.dto.NotificationDto
import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.domain.model.NotificationType

/**
 * Mapper functions for Notification data conversion.
 */
object NotificationMapper {
    
    fun NotificationDto.toDomain(): Notification {
        return Notification(
            notificationId = notificationId,
            userId = actorId,
            username = actorUsername,
            userProfileImage = actorProfileImage,
            targetUserId = recipientId,
            type = NotificationType.fromString(type),
            postId = postId,
            postThumbnail = postThumbnail,
            commentText = commentText,
            timestamp = createdAt,
            isRead = isRead,
            isFollowing = false,  // Will be enriched later
            hasStory = false      // Will be enriched later
        )
    }
    
    fun Notification.toDto(): NotificationDto {
        return NotificationDto(
            notificationId = notificationId,
            recipientId = targetUserId,
            actorId = userId,
            actorUsername = username,
            actorProfileImage = userProfileImage,
            type = type.toApiString(),
            postId = postId,
            postThumbnail = postThumbnail,
            commentText = commentText,
            isRead = isRead,
            createdAt = timestamp
        )
    }
}

/**
 * Extension to convert NotificationType to API string
 */
fun NotificationType.toApiString(): String {
    return when (this) {
        NotificationType.LIKE -> "like"
        NotificationType.COMMENT -> "comment"
        NotificationType.FOLLOW -> "follow"
        NotificationType.FOLLOW_REQUEST -> "follow_request"
        NotificationType.MENTION -> "mention"
        NotificationType.LIKE_COMMENT -> "like_comment"
        NotificationType.TAG -> "tag"
    }
}
