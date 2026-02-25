package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.Conversation
import com.junaidjamshid.i211203.domain.model.Message
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for messaging operations.
 */
interface MessageRepository {
    
    fun getConversations(userId: String): Flow<Resource<List<Conversation>>>
    
    fun getMessages(conversationId: String): Flow<Resource<List<Message>>>
    
    suspend fun sendMessage(message: Message): Resource<Message>
    
    suspend fun sendImageMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        imageBase64: String
    ): Resource<Message>
    
    suspend fun deleteMessage(messageId: String, conversationId: String): Resource<Unit>
    
    suspend fun markMessageAsRead(messageId: String, conversationId: String): Resource<Unit>
    
    suspend fun markAllMessagesAsRead(conversationId: String, userId: String): Resource<Unit>
    
    suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String
    ): Resource<Conversation>
    
    suspend fun deleteConversation(conversationId: String): Resource<Unit>
    
    suspend fun setVanishMode(conversationId: String, enabled: Boolean): Resource<Unit>
}
