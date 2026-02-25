package com.junaidjamshid.i211203.presentation.chat

import com.junaidjamshid.i211203.domain.model.Message
import com.junaidjamshid.i211203.domain.model.User

/**
 * UI State for Chat screens.
 */
data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val otherUser: User? = null,
    val isVanishModeEnabled: Boolean = false,
    val messageSent: Boolean = false,
    val error: String? = null
)

/**
 * UI State for conversations list (DMs).
 */
data class ConversationsUiState(
    val isLoading: Boolean = false,
    val conversations: List<ConversationItem> = emptyList(),
    val error: String? = null
)

/**
 * Represents a conversation item in the list.
 */
data class ConversationItem(
    val conversationId: String,
    val otherUser: User,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0
)
