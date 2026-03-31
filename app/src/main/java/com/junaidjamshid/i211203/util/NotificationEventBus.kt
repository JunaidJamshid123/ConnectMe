package com.junaidjamshid.i211203.util

import com.junaidjamshid.i211203.domain.model.Notification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event bus for notification events that need to be propagated
 * across the app for reactive UI updates.
 * 
 * This provides a way to notify different parts of the app
 * when notifications are created, received, or read.
 */
@Singleton
class NotificationEventBus @Inject constructor() {
    
    // Flow for new notifications received
    private val _newNotifications = MutableSharedFlow<Notification>(extraBufferCapacity = 10)
    val newNotifications: SharedFlow<Notification> = _newNotifications.asSharedFlow()
    
    // Flow for notification count changes
    private val _unreadCountChanged = MutableSharedFlow<Int>(extraBufferCapacity = 10, replay = 1)
    val unreadCountChanged: SharedFlow<Int> = _unreadCountChanged.asSharedFlow()
    
    // Flow for when all notifications are marked as read
    private val _allRead = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val allRead: SharedFlow<Unit> = _allRead.asSharedFlow()
    
    // Flow for when a specific notification is read
    private val _notificationRead = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val notificationRead: SharedFlow<String> = _notificationRead.asSharedFlow()
    
    /**
     * Emit a new notification event
     */
    suspend fun emitNewNotification(notification: Notification) {
        _newNotifications.emit(notification)
    }
    
    /**
     * Try to emit a new notification (non-suspending)
     */
    fun tryEmitNewNotification(notification: Notification) {
        _newNotifications.tryEmit(notification)
    }
    
    /**
     * Emit unread count change
     */
    suspend fun emitUnreadCountChanged(count: Int) {
        _unreadCountChanged.emit(count)
    }
    
    /**
     * Try to emit unread count change (non-suspending)
     */
    fun tryEmitUnreadCountChanged(count: Int) {
        _unreadCountChanged.tryEmit(count)
    }
    
    /**
     * Emit all read event
     */
    suspend fun emitAllRead() {
        _allRead.emit(Unit)
        _unreadCountChanged.emit(0)
    }
    
    /**
     * Emit notification read event
     */
    suspend fun emitNotificationRead(notificationId: String) {
        _notificationRead.emit(notificationId)
    }
}
