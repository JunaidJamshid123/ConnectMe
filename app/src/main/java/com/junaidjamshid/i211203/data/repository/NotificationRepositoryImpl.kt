package com.junaidjamshid.i211203.data.repository

import android.util.Log
import com.junaidjamshid.i211203.data.mapper.NotificationMapper.toDomain
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseNotificationDataSource
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseUserDataSource
import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.domain.repository.NotificationRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationRepository using Supabase.
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDataSource: SupabaseNotificationDataSource,
    private val userDataSource: SupabaseUserDataSource
) : NotificationRepository {
    
    override suspend fun getNotifications(): Resource<List<Notification>> {
        return try {
            val userId = notificationDataSource.getCurrentUserId()
                ?: return Resource.Error("User not logged in")
            
            val notificationDtos = notificationDataSource.getNotifications(userId)
            
            // Enrich with following status
            val followingIds = try {
                userDataSource.getFollowing(userId).toSet()
            } catch (e: Exception) {
                emptySet()
            }
            
            val notifications = notificationDtos.map { dto ->
                dto.toDomain().copy(
                    isFollowing = followingIds.contains(dto.actorId)
                )
            }
            
            Resource.Success(notifications)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get notifications")
        }
    }
    
    override fun getNotificationsFlow(): Flow<Resource<List<Notification>>> = flow {
        emit(Resource.Loading())
        
        // Initial load
        emit(getNotifications())
        
        // Auto-refresh every 30 seconds when subscribed
        while (true) {
            delay(30000)
            emit(getNotifications())
        }
    }
    
    override suspend fun getUnreadCount(): Resource<Int> {
        return try {
            val userId = notificationDataSource.getCurrentUserId()
                ?: return Resource.Error("User not logged in")
            
            val count = notificationDataSource.getUnreadCount(userId)
            Resource.Success(count)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get unread count")
        }
    }
    
    override fun getUnreadCountFlow(): Flow<Int> = flow {
        val userId = notificationDataSource.getCurrentUserId() ?: return@flow
        
        // Emit initial count
        emit(notificationDataSource.getUnreadCount(userId))
        
        // Refresh count every 10 seconds
        while (true) {
            delay(10000)
            emit(notificationDataSource.getUnreadCount(userId))
        }
    }
    
    override suspend fun createLikeNotification(
        postOwnerId: String,
        postId: String,
        postThumbnail: String?
    ): Resource<Unit> {
        return try {
            val currentUser = getCurrentUser() ?: return Resource.Error("User not logged in")
            
            notificationDataSource.createNotification(
                recipientId = postOwnerId,
                actorId = currentUser.userId,
                actorUsername = currentUser.username,
                actorProfileImage = currentUser.profilePicture ?: "",
                type = "like",
                postId = postId,
                postThumbnail = postThumbnail
            )
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create notification")
        }
    }
    
    override suspend fun createCommentNotification(
        postOwnerId: String,
        postId: String,
        postThumbnail: String?,
        commentText: String
    ): Resource<Unit> {
        return try {
            val currentUser = getCurrentUser() ?: return Resource.Error("User not logged in")
            
            notificationDataSource.createNotification(
                recipientId = postOwnerId,
                actorId = currentUser.userId,
                actorUsername = currentUser.username,
                actorProfileImage = currentUser.profilePicture ?: "",
                type = "comment",
                postId = postId,
                postThumbnail = postThumbnail,
                commentText = commentText.take(100) // Truncate long comments
            )
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create notification")
        }
    }
    
    override suspend fun createFollowNotification(
        followedUserId: String
    ): Resource<Unit> {
        return try {
            val currentUser = getCurrentUser() ?: return Resource.Error("User not logged in")
            
            notificationDataSource.createNotification(
                recipientId = followedUserId,
                actorId = currentUser.userId,
                actorUsername = currentUser.username,
                actorProfileImage = currentUser.profilePicture ?: "",
                type = "follow"
            )
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create notification")
        }
    }
    
    override suspend fun createMentionNotification(
        mentionedUserId: String,
        postId: String,
        postThumbnail: String?
    ): Resource<Unit> {
        return try {
            val currentUser = getCurrentUser() ?: return Resource.Error("User not logged in")
            
            notificationDataSource.createNotification(
                recipientId = mentionedUserId,
                actorId = currentUser.userId,
                actorUsername = currentUser.username,
                actorProfileImage = currentUser.profilePicture ?: "",
                type = "mention",
                postId = postId,
                postThumbnail = postThumbnail
            )
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create notification")
        }
    }
    
    override suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            notificationDataSource.markAsRead(notificationId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark as read")
        }
    }
    
    override suspend fun markAllAsRead(): Resource<Unit> {
        return try {
            val userId = notificationDataSource.getCurrentUserId()
                ?: return Resource.Error("User not logged in")
            
            notificationDataSource.markAllAsRead(userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark all as read")
        }
    }
    
    override suspend fun deleteNotification(notificationId: String): Resource<Unit> {
        return try {
            notificationDataSource.deleteNotification(notificationId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete notification")
        }
    }
    
    override fun subscribeToNewNotifications(): Flow<Notification> {
        val userId = notificationDataSource.getCurrentUserId()
        Log.d("NotificationRepoImpl", "subscribeToNewNotifications - userId: $userId")
        
        if (userId == null) {
            Log.e("NotificationRepoImpl", "Cannot subscribe - no user logged in!")
            return flow { }
        }
        
        return notificationDataSource.subscribeToNotifications(userId)
            .map { dto -> 
                Log.d("NotificationRepoImpl", "Received notification in repo: ${dto.type}")
                dto.toDomain() 
            }
    }
    
    /**
     * Helper to get current user info
     */
    private suspend fun getCurrentUser(): CurrentUserInfo? {
        val userId = notificationDataSource.getCurrentUserId() ?: return null
        val userDto = userDataSource.getUserById(userId) ?: return null
        return CurrentUserInfo(
            userId = userId,
            username = userDto.username,
            profilePicture = userDto.profilePicture
        )
    }
    
    private data class CurrentUserInfo(
        val userId: String,
        val username: String,
        val profilePicture: String?
    )
}
