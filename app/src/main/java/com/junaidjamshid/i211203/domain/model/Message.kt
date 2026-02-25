package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Message in the application.
 */
data class Message(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val messageType: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    VIDEO
}

/**
 * Domain model representing a Conversation.
 */
data class Conversation(
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0,
    val unreadCount: Int = 0,
    val otherUser: User? = null
)
