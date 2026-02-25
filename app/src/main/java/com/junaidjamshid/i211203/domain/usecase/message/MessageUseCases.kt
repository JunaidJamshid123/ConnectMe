package com.junaidjamshid.i211203.domain.usecase.message

import com.junaidjamshid.i211203.domain.model.Conversation
import com.junaidjamshid.i211203.domain.model.Message
import com.junaidjamshid.i211203.domain.repository.MessageRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get user's conversations.
 */
class GetConversationsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(userId: String): Flow<Resource<List<Conversation>>> {
        return messageRepository.getConversations(userId)
    }
}

/**
 * Use case to get messages in a conversation.
 */
class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(conversationId: String): Flow<Resource<List<Message>>> {
        return messageRepository.getMessages(conversationId)
    }
}

/**
 * Use case to send a message.
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(message: Message): Resource<Message> {
        if (message.content.isBlank() && message.imageUrl.isNullOrBlank()) {
            return Resource.Error("Message cannot be empty")
        }
        return messageRepository.sendMessage(message)
    }
}
