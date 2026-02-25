package com.junaidjamshid.i211203.data.dto

/**
 * Data Transfer Object for Message data from Firebase.
 */
data class MessageDto(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val messageType: String = "TEXT"
)

/**
 * Data Transfer Object for Conversation data from Firebase.
 */
data class ConversationDto(
    val conversationId: String = "",
    val participants: MutableMap<String, Boolean> = mutableMapOf(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0,
    val lastMessageSenderId: String = ""
)
