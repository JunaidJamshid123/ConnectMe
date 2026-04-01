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
    val error: String? = null,
    // Typing indicator
    val isOtherUserTyping: Boolean = false,
    // Online status
    val isOtherUserOnline: Boolean = false,
    val lastSeenTimestamp: Long? = null,
    // Voice recording state
    val isRecording: Boolean = false,
    val recordingDuration: Long = 0,
    val isSendingMedia: Boolean = false,
    // Image preview state
    val selectedImageUri: String? = null,
    val selectedImageBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatUiState

        if (isLoading != other.isLoading) return false
        if (messages != other.messages) return false
        if (otherUser != other.otherUser) return false
        if (isVanishModeEnabled != other.isVanishModeEnabled) return false
        if (messageSent != other.messageSent) return false
        if (error != other.error) return false
        if (isOtherUserTyping != other.isOtherUserTyping) return false
        if (isOtherUserOnline != other.isOtherUserOnline) return false
        if (lastSeenTimestamp != other.lastSeenTimestamp) return false
        if (isRecording != other.isRecording) return false
        if (recordingDuration != other.recordingDuration) return false
        if (isSendingMedia != other.isSendingMedia) return false
        if (selectedImageUri != other.selectedImageUri) return false
        if (selectedImageBytes != null) {
            if (other.selectedImageBytes == null) return false
            if (!selectedImageBytes.contentEquals(other.selectedImageBytes)) return false
        } else if (other.selectedImageBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isLoading.hashCode()
        result = 31 * result + messages.hashCode()
        result = 31 * result + (otherUser?.hashCode() ?: 0)
        result = 31 * result + isVanishModeEnabled.hashCode()
        result = 31 * result + messageSent.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + isOtherUserTyping.hashCode()
        result = 31 * result + isOtherUserOnline.hashCode()
        result = 31 * result + (lastSeenTimestamp?.hashCode() ?: 0)
        result = 31 * result + isRecording.hashCode()
        result = 31 * result + recordingDuration.hashCode()
        result = 31 * result + isSendingMedia.hashCode()
        result = 31 * result + (selectedImageUri?.hashCode() ?: 0)
        result = 31 * result + (selectedImageBytes?.contentHashCode() ?: 0)
        return result
    }
}

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
