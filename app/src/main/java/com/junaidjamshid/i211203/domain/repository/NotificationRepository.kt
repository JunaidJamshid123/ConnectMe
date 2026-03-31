package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notification-related operations.
 */
interface NotificationRepository {
    
    /**
     * Get all notifications for the current user
     */
    suspend fun getNotifications(): Resource<List<Notification>>
    
    /**
     * Get notifications as a Flow (for real-time updates in UI)
     */
    fun getNotificationsFlow(): Flow<Resource<List<Notification>>>
    
    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(): Resource<Int>
    
    /**
     * Get unread count as a Flow (for badge updates)
     */
    fun getUnreadCountFlow(): Flow<Int>
    
    /**
     * Create a like notification
     */
    suspend fun createLikeNotification(
        postOwnerId: String,
        postId: String,
        postThumbnail: String?
    ): Resource<Unit>
    
    /**
     * Create a comment notification
     */
    suspend fun createCommentNotification(
        postOwnerId: String,
        postId: String,
        postThumbnail: String?,
        commentText: String
    ): Resource<Unit>
    
    /**
     * Create a follow notification
     */
    suspend fun createFollowNotification(
        followedUserId: String
    ): Resource<Unit>
    
    /**
     * Create a mention notification
     */
    suspend fun createMentionNotification(
        mentionedUserId: String,
        postId: String,
        postThumbnail: String?
    ): Resource<Unit>
    
    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String): Resource<Unit>
    
    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(): Resource<Unit>
    
    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String): Resource<Unit>
    
    /**
     * Subscribe to real-time notifications
     * Returns a Flow that emits new notifications as they arrive
     */
    fun subscribeToNewNotifications(): Flow<Notification>
}
