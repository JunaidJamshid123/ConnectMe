package com.junaidjamshid.i211203.presentation.contacts

import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Contacts screens.
 */
data class ContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactItem> = emptyList(),
    val error: String? = null
)

/**
 * Represents a contact item with last message info.
 */
data class ContactItem(
    val user: User,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)
