package com.junaidjamshid.i211203.presentation.messages

import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for DMs Activity.
 */
data class DmsUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val friends: List<DmsFriendItem> = emptyList(),
    val filteredFriends: List<DmsFriendItem> = emptyList(),
    val error: String? = null
)

/**
 * Represents a friend item in the DMs list.
 */
data class DmsFriendItem(
    val user: User,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

/**
 * Data class for notes/stories row in DMs
 */
data class DmsNoteUser(
    val userId: String,
    val username: String,
    val profilePicture: String?,
    val isOnline: Boolean = false,
    val isCurrentUser: Boolean = false,
    val noteText: String? = null
)
