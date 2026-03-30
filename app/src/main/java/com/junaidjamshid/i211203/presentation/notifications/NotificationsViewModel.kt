package com.junaidjamshid.i211203.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.junaidjamshid.i211203.domain.model.Notification
import com.junaidjamshid.i211203.domain.model.NotificationSection
import com.junaidjamshid.i211203.domain.model.NotificationType
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.domain.usecase.auth.GetCurrentUserUseCase
import com.junaidjamshid.i211203.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Notifications screen
 */
data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val groupedNotifications: Map<NotificationSection, List<Notification>> = emptyMap(),
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val currentUserId: String?
        get() = getCurrentUserUseCase.getCurrentUserId()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Simulate loading with sample data
            // In production, this would call a NotificationRepository
            delay(800)

            val sampleNotifications = generateSampleNotifications()
            val grouped = groupNotificationsByTime(sampleNotifications)

            _uiState.update {
                it.copy(
                    notifications = sampleNotifications,
                    groupedNotifications = grouped,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
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
        _uiState.update { state ->
            val updatedNotifications = state.notifications.map { notification ->
                if (notification.notificationId == notificationId) {
                    notification.copy(isRead = true)
                } else notification
            }
            state.copy(
                notifications = updatedNotifications,
                groupedNotifications = groupNotificationsByTime(updatedNotifications)
            )
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

    /**
     * Generate sample notifications for UI preview.
     * In production, this would fetch from a repository.
     */
    private fun generateSampleNotifications(): List<Notification> {
        val now = System.currentTimeMillis()
        val hour = 60 * 60 * 1000L
        val day = 24 * hour

        return listOf(
            // Today
            Notification(
                notificationId = "1",
                userId = "user1",
                username = "sarah_designs",
                userProfileImage = "",
                type = NotificationType.LIKE,
                postId = "post1",
                postThumbnail = "",
                timestamp = now - (2 * hour),
                hasStory = true
            ),
            Notification(
                notificationId = "2",
                userId = "user2",
                username = "mike_photo",
                userProfileImage = "",
                type = NotificationType.COMMENT,
                postId = "post2",
                postThumbnail = "",
                commentText = "This is amazing! 🔥",
                timestamp = now - (4 * hour)
            ),
            Notification(
                notificationId = "3",
                userId = "user3",
                username = "jessica_travel",
                userProfileImage = "",
                type = NotificationType.FOLLOW,
                timestamp = now - (6 * hour),
                isFollowing = false
            ),
            Notification(
                notificationId = "4",
                userId = "user4",
                username = "alex_music",
                userProfileImage = "",
                type = NotificationType.LIKE,
                postId = "post3",
                postThumbnail = "",
                timestamp = now - (8 * hour),
                hasStory = true
            ),

            // This Week
            Notification(
                notificationId = "5",
                userId = "user5",
                username = "emma_fitness",
                userProfileImage = "",
                type = NotificationType.FOLLOW,
                timestamp = now - (2 * day),
                isFollowing = true
            ),
            Notification(
                notificationId = "6",
                userId = "user6",
                username = "david_art",
                userProfileImage = "",
                type = NotificationType.COMMENT,
                postId = "post4",
                postThumbnail = "",
                commentText = "Love the colors!",
                timestamp = now - (3 * day)
            ),
            Notification(
                notificationId = "7",
                userId = "user7",
                username = "lisa_food",
                userProfileImage = "",
                type = NotificationType.MENTION,
                postId = "post5",
                postThumbnail = "",
                timestamp = now - (4 * day)
            ),
            Notification(
                notificationId = "8",
                userId = "user8",
                username = "tom_tech",
                userProfileImage = "",
                type = NotificationType.LIKE,
                postId = "post6",
                postThumbnail = "",
                timestamp = now - (5 * day),
                hasStory = true
            ),

            // This Month
            Notification(
                notificationId = "9",
                userId = "user9",
                username = "anna_fashion",
                userProfileImage = "",
                type = NotificationType.FOLLOW,
                timestamp = now - (10 * day),
                isFollowing = false
            ),
            Notification(
                notificationId = "10",
                userId = "user10",
                username = "james_sports",
                userProfileImage = "",
                type = NotificationType.LIKE,
                postId = "post7",
                postThumbnail = "",
                timestamp = now - (15 * day)
            ),

            // Earlier
            Notification(
                notificationId = "11",
                userId = "user11",
                username = "olivia_pets",
                userProfileImage = "",
                type = NotificationType.COMMENT,
                postId = "post8",
                postThumbnail = "",
                commentText = "So cute! 🐶",
                timestamp = now - (35 * day)
            ),
            Notification(
                notificationId = "12",
                userId = "user12",
                username = "chris_nature",
                userProfileImage = "",
                type = NotificationType.FOLLOW,
                timestamp = now - (40 * day),
                isFollowing = true
            )
        )
    }
}
