package com.junaidjamshid.i211203.data.remote.supabase

import android.util.Log
import com.junaidjamshid.i211203.data.dto.NotificationDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase representation of Notification for serialization.
 */
@Serializable
data class SupabaseNotification(
    val notification_id: String = "",
    val recipient_id: String = "",
    val actor_id: String = "",
    val actor_username: String = "",
    val actor_profile_image: String = "",
    val type: String = "",
    val post_id: String? = null,
    val post_thumbnail: String? = null,
    val story_id: String? = null,
    val comment_id: String? = null,
    val comment_text: String? = null,
    val is_read: Boolean = false,
    val created_at: Long = 0
)

/**
 * Insert model for notifications (without notification_id, it's auto-generated)
 */
@Serializable
data class SupabaseNotificationInsert(
    val recipient_id: String,
    val actor_id: String,
    val actor_username: String,
    val actor_profile_image: String = "",
    val type: String,
    val post_id: String? = null,
    val post_thumbnail: String? = null,
    val story_id: String? = null,
    val comment_id: String? = null,
    val comment_text: String? = null
)

fun SupabaseNotification.toDto(): NotificationDto = NotificationDto(
    notificationId = notification_id,
    recipientId = recipient_id,
    actorId = actor_id,
    actorUsername = actor_username,
    actorProfileImage = actor_profile_image,
    type = type,
    postId = post_id,
    postThumbnail = post_thumbnail,
    storyId = story_id,
    commentId = comment_id,
    commentText = comment_text,
    isRead = is_read,
    createdAt = created_at
)

/**
 * Data source for Supabase Notification operations.
 */
@Singleton
class SupabaseNotificationDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val TAG = "SupabaseNotificationDS"
    
    /**
     * Get all notifications for a user, ordered by most recent
     */
    suspend fun getNotifications(userId: String): List<NotificationDto> {
        return try {
            val notifications = supabaseClient.postgrest[SupabaseConfig.NOTIFICATIONS_TABLE]
                .select {
                    filter {
                        eq("recipient_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(100)
                }
                .decodeList<SupabaseNotification>()
            
            notifications.map { it.toDto() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notifications: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get unread notification count for a user
     */
    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val notifications = supabaseClient.postgrest[SupabaseConfig.NOTIFICATIONS_TABLE]
                .select {
                    filter {
                        eq("recipient_id", userId)
                        eq("is_read", false)
                    }
                }
                .decodeList<SupabaseNotification>()
            
            notifications.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count: ${e.message}")
            0
        }
    }
    
    /**
     * Create a new notification
     */
    suspend fun createNotification(
        recipientId: String,
        actorId: String,
        actorUsername: String,
        actorProfileImage: String,
        type: String,
        postId: String? = null,
        postThumbnail: String? = null,
        storyId: String? = null,
        commentId: String? = null,
        commentText: String? = null
    ) {
        try {
            // Don't create notification for self
            if (recipientId == actorId) return
            
            val notification = SupabaseNotificationInsert(
                recipient_id = recipientId,
                actor_id = actorId,
                actor_username = actorUsername,
                actor_profile_image = actorProfileImage,
                type = type,
                post_id = postId,
                post_thumbnail = postThumbnail,
                story_id = storyId,
                comment_id = commentId,
                comment_text = commentText
            )
            
            supabaseClient.postgrest[SupabaseConfig.NOTIFICATIONS_TABLE]
                .insert(notification)
            
            Log.d(TAG, "Notification created: $type for $recipientId")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification: ${e.message}")
        }
    }
    
    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String) {
        try {
            supabaseClient.postgrest[SupabaseConfig.NOTIFICATIONS_TABLE]
                .update({
                    set("is_read", true)
                }) {
                    filter {
                        eq("notification_id", notificationId)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read: ${e.message}")
        }
    }
    
    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String) {
        try {
            supabaseClient.postgrest[SupabaseConfig.NOTIFICATIONS_TABLE]
                .update({
                    set("is_read", true)
                }) {
                    filter {
                        eq("recipient_id", userId)
                        eq("is_read", false)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read: ${e.message}")
        }
    }
    
    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String) {
        try {
            supabaseClient.postgrest[SupabaseConfig.NOTIFICATIONS_TABLE]
                .delete {
                    filter {
                        eq("notification_id", notificationId)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification: ${e.message}")
        }
    }
    
    /**
     * Delete notifications for a specific post (when post is deleted)
     */
    suspend fun deleteNotificationsForPost(postId: String) {
        try {
            supabaseClient.postgrest[SupabaseConfig.NOTIFICATIONS_TABLE]
                .delete {
                    filter {
                        eq("post_id", postId)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notifications for post: ${e.message}")
        }
    }
    
    /**
     * Subscribe to real-time notifications for a user
     * Returns a Flow that emits new notifications
     */
    fun subscribeToNotifications(userId: String): Flow<NotificationDto> = callbackFlow {
        Log.d(TAG, "========================================")
        Log.d(TAG, "Setting up realtime subscription for user: $userId")
        Log.d(TAG, "Realtime status BEFORE connect: ${supabaseClient.realtime.status.value}")
        Log.d(TAG, "========================================")
        
        // Ensure realtime is connected
        try {
            supabaseClient.realtime.connect()
            Log.d(TAG, "Realtime connected successfully")
        } catch (e: Exception) {
            Log.d(TAG, "Realtime connect note: ${e.message}")
        }
        Log.d(TAG, "Realtime status AFTER connect: ${supabaseClient.realtime.status.value}")
        
        val channelName = "notifications_$userId"
        Log.d(TAG, "Creating channel: $channelName")
        val channel = supabaseClient.realtime.channel(channelName)
        
        try {
            Log.d(TAG, "Setting up postgres change flow for table: ${SupabaseConfig.NOTIFICATIONS_TABLE}")
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(
                schema = "public"
            ) {
                table = SupabaseConfig.NOTIFICATIONS_TABLE
            }
            
            // Subscribe to the channel
            Log.d(TAG, "About to subscribe to channel...")
            channel.subscribe()
            Log.d(TAG, ">>> SUBSCRIBED TO NOTIFICATION CHANNEL <<<")
            Log.d(TAG, "Channel status after subscribe: ${channel.status.value}")
            
            // Collect changes in a separate coroutine
            launch {
                changeFlow.collect { change ->
                    try {
                        val record = change.record
                        Log.d(TAG, "Received realtime change: $record")
                        
                        // Filter for the specific user on the client side
                        val recipientId = record["recipient_id"]?.toString() ?: ""
                        if (recipientId != userId) {
                            Log.d(TAG, "Skipping notification for different user")
                            return@collect
                        }
                        
                        val notification = SupabaseNotification(
                            notification_id = record["notification_id"]?.toString() ?: "",
                            recipient_id = recipientId,
                            actor_id = record["actor_id"]?.toString() ?: "",
                            actor_username = record["actor_username"]?.toString() ?: "",
                            actor_profile_image = record["actor_profile_image"]?.toString() ?: "",
                            type = record["type"]?.toString() ?: "",
                            post_id = record["post_id"]?.toString(),
                            post_thumbnail = record["post_thumbnail"]?.toString(),
                            story_id = record["story_id"]?.toString(),
                            comment_id = record["comment_id"]?.toString(),
                            comment_text = record["comment_text"]?.toString(),
                            is_read = record["is_read"]?.toString()?.toBoolean() ?: false,
                            created_at = record["created_at"]?.toString()?.toLongOrNull() 
                                ?: System.currentTimeMillis()
                        )
                        
                        Log.d(TAG, "Emitting notification: ${notification.type} from ${notification.actor_username}")
                        trySend(notification.toDto())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing realtime notification: ${e.message}", e)
                    }
                }
            }
            
            // Keep the flow alive
            awaitClose {
                Log.d(TAG, "Closing notification channel")
                launch {
                    try {
                        channel.unsubscribe()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error unsubscribing: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notification subscription: ${e.message}", e)
            close(e)
        }
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }
}
