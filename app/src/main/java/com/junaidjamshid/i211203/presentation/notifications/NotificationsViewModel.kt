package com.junaidjamshid.i211203.presentation.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.domain.model.NotificationSection
import com.junaidjamshid.i211203.domain.repository.NotificationRepository
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.domain.usecase.auth.GetCurrentUserUseCase
import com.junaidjamshid.i211203.util.NotificationEventBus
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Notifications screen
 */
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val groupedNotifications: Map<NotificationSection, List<Notification>> = emptyMap(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Notifications screen
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationEventBus: NotificationEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = getCurrentUserUseCase.getCurrentUserId()

    init {
        loadNotifications()
        subscribeToNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = notificationRepository.getNotifications()) {
                is Resource.Success -> {
                    val notifications = result.data ?: emptyList()
                    val grouped = groupNotificationsByTime(notifications)
                    _uiState.update {
                        it.copy(
                            notifications = notifications,
                            groupedNotifications = grouped,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.message
                        )
                    }
                }
                is Resource.Loading -> {
                    // Already showing loading
                }
            }

            // Load unread count
            loadUnreadCount()
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            when (val result = notificationRepository.getUnreadCount()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(unreadCount = result.data ?: 0) }
                }
                else -> { /* Ignore errors for count */ }
            }
        }
    }

    private fun subscribeToNotifications() {
        notificationRepository.subscribeToNewNotifications()
            .onEach { notification ->
                Log.d("NotificationsViewModel", "New notification received: ${notification.type} from ${notification.username}")
                
                // Add new notification to the list
                val currentNotifications = _uiState.value.notifications.toMutableList()
                // Check if notification already exists
                if (currentNotifications.none { it.notificationId == notification.notificationId }) {
                    currentNotifications.add(0, notification)
                    val grouped = groupNotificationsByTime(currentNotifications)
                    _uiState.update {
                        it.copy(
                            notifications = currentNotifications,
                            groupedNotifications = grouped,
                            unreadCount = it.unreadCount + 1
                        )
                    }
                }
            }
            .catch { e ->
                Log.e("NotificationsViewModel", "Error in notification subscription: ${e.message}")
            }
            .launchIn(viewModelScope)
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadNotifications()
        }
    }

    fun onFollowClick(userId: String) {
        val currentUserId = this.currentUserId ?: return

        // Toggle follow state optimistically
        _uiState.update { state ->
            val updatedNotifications = state.notifications.map { notification ->
                if (notification.userId == userId) {
                    notification.copy(isFollowing = !notification.isFollowing)
                } else notification
            }
            state.copy(
                notifications = updatedNotifications,
                groupedNotifications = groupNotificationsByTime(updatedNotifications)
            )
        }

        // Perform actual follow/unfollow
        viewModelScope.launch {
            val notification = _uiState.value.notifications.find { it.userId == userId }
            if (notification?.isFollowing == true) {
                userRepository.followUser(currentUserId, userId)
            } else {
                userRepository.unfollowUser(currentUserId, userId)
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            val userId = currentUserId ?: return@launch
            
            // Update locally first (optimistic)
            _uiState.update { state ->
                val updatedNotifications = state.notifications.map { notification ->
                    if (notification.notificationId == notificationId) {
                        notification.copy(isRead = true)
                    } else notification
                }
                state.copy(
                    notifications = updatedNotifications,
                    groupedNotifications = groupNotificationsByTime(updatedNotifications),
                    unreadCount = (state.unreadCount - 1).coerceAtLeast(0)
                )
            }
            
            // Update on server
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            // Update locally first (optimistic)
            _uiState.update { state ->
                val updatedNotifications = state.notifications.map { it.copy(isRead = true) }
                state.copy(
                    notifications = updatedNotifications,
                    groupedNotifications = groupNotificationsByTime(updatedNotifications),
                    unreadCount = 0
                )
            }
            
            // Emit to event bus so other screens can update
            notificationEventBus.emitAllRead()
            
            // Update on server
            notificationRepository.markAllAsRead()
        }
    }

    private fun groupNotificationsByTime(notifications: List<Notification>): Map<NotificationSection, List<Notification>> {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val oneWeekMs = 7 * oneDayMs
        val oneMonthMs = 30 * oneDayMs

        return notifications.groupBy { notification ->
            val age = now - notification.timestamp
            when {
                age < oneDayMs -> NotificationSection.TODAY
                age < oneWeekMs -> NotificationSection.THIS_WEEK
                age < oneMonthMs -> NotificationSection.THIS_MONTH
                else -> NotificationSection.EARLIER
            }
        }.toSortedMap(compareBy { it.ordinal })
    }
}
