package com.junaidjamshid.i211203.data.remote.supabase

import com.junaidjamshid.i211203.data.dto.MessageDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase representation of Message.
 */
@Serializable
data class SupabaseMessage(
    val message_id: String = "",
    val conversation_id: String = "",
    val sender_id: String = "",
    val receiver_id: String = "",
    val content: String = "",
    val image_url: String? = null,
    val timestamp: Long = 0,
    val is_read: Boolean = false,
    val is_deleted: Boolean = false,
    val message_type: String = "TEXT"
)

/**
 * Supabase representation of Conversation metadata.
 */
@Serializable
data class SupabaseConversation(
    val conversation_id: String = "",
    val participant_1: String = "",
    val participant_2: String = "",
    val last_message: String = "",
    val last_message_timestamp: Long = 0,
    val last_message_sender_id: String = ""
)

fun SupabaseMessage.toDto(): MessageDto = MessageDto(
    messageId = message_id,
    conversationId = conversation_id,
    senderId = sender_id,
    receiverId = receiver_id,
    content = content,
    imageUrl = image_url,
    timestamp = timestamp,
    isRead = is_read,
    isDeleted = is_deleted,
    messageType = message_type
)

/**
 * Data source for Supabase Message operations.
 */
@Singleton
class SupabaseMessageDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    private fun getCurrentUserId(): String? = supabaseClient.auth.currentUserOrNull()?.id
    
    private fun getConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
    
    /**
     * Get conversations for current user as a Flow
     */
    fun getConversations(): Flow<List<MessageDto>> = flow {
        val currentUserId = getCurrentUserId() ?: return@flow
        
        emit(getConversationsList(currentUserId))
        
        val channel = supabaseClient.realtime.channel("conversations-$currentUserId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.CONVERSATIONS_TABLE
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getConversationsList(currentUserId))
        }
    }
    
    private suspend fun getConversationsList(currentUserId: String): List<MessageDto> {
        // Get conversations where user is a participant
        val conversations = supabaseClient.postgrest[SupabaseConfig.CONVERSATIONS_TABLE]
            .select {
                filter {
                    or {
                        eq("participant_1", currentUserId)
                        eq("participant_2", currentUserId)
                    }
                }
                order("last_message_timestamp", Order.DESCENDING)
            }
            .decodeList<SupabaseConversation>()
        
        // For each conversation, get the last message
        return conversations.mapNotNull { conv ->
            val lastMessage = supabaseClient.postgrest[SupabaseConfig.MESSAGES_TABLE]
                .select {
                    filter {
                        eq("conversation_id", conv.conversation_id)
                    }
                    order("timestamp", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<SupabaseMessage>()
            lastMessage?.toDto()
        }
    }
    
    /**
     * Get messages for a specific conversation as a Flow
     */
    fun getMessages(conversationId: String): Flow<List<MessageDto>> = flow {
        emit(getMessagesList(conversationId))
        
        val channel = supabaseClient.realtime.channel("messages-$conversationId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.MESSAGES_TABLE
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getMessagesList(conversationId))
        }
    }
    
    private suspend fun getMessagesList(conversationId: String): List<MessageDto> {
        val messages = supabaseClient.postgrest[SupabaseConfig.MESSAGES_TABLE]
            .select {
                filter {
                    eq("conversation_id", conversationId)
                    eq("is_deleted", false)
                }
                order("timestamp", Order.ASCENDING)
            }
            .decodeList<SupabaseMessage>()
        return messages.map { it.toDto() }
    }
    
    /**
     * Send a message
     */
    suspend fun sendMessage(
        receiverId: String,
        messageText: String,
        messageType: String = "TEXT",
        mediaUrl: String? = null
    ): Result<MessageDto> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))
            
            val conversationId = getConversationId(currentUserId, receiverId)
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            
            val supabaseMessage = SupabaseMessage(
                message_id = messageId,
                conversation_id = conversationId,
                sender_id = currentUserId,
                receiver_id = receiverId,
                content = messageText,
                image_url = mediaUrl,
                timestamp = timestamp,
                is_read = false,
                is_deleted = false,
                message_type = messageType.uppercase()
            )
            
            // Insert message
            supabaseClient.postgrest[SupabaseConfig.MESSAGES_TABLE]
                .insert(supabaseMessage)
            
            // Update or create conversation metadata
            updateConversationMetadata(conversationId, currentUserId, receiverId, messageText, timestamp)
            
            Result.success(supabaseMessage.toDto())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun updateConversationMetadata(
        conversationId: String,
        senderId: String,
        receiverId: String,
        lastMessage: String,
        timestamp: Long
    ) {
        // Check if conversation exists
        val existing = supabaseClient.postgrest[SupabaseConfig.CONVERSATIONS_TABLE]
            .select {
                filter {
                    eq("conversation_id", conversationId)
                }
            }
            .decodeList<SupabaseConversation>()
        
        if (existing.isEmpty()) {
            // Create new conversation
            val conversation = SupabaseConversation(
                conversation_id = conversationId,
                participant_1 = if (senderId < receiverId) senderId else receiverId,
                participant_2 = if (senderId < receiverId) receiverId else senderId,
                last_message = lastMessage,
                last_message_timestamp = timestamp,
                last_message_sender_id = senderId
            )
            supabaseClient.postgrest[SupabaseConfig.CONVERSATIONS_TABLE]
                .insert(conversation)
        } else {
            // Update existing conversation
            supabaseClient.postgrest[SupabaseConfig.CONVERSATIONS_TABLE]
                .update({
                    set("last_message", lastMessage)
                    set("last_message_timestamp", timestamp)
                    set("last_message_sender_id", senderId)
                }) {
                    filter {
                        eq("conversation_id", conversationId)
                    }
                }
        }
    }
    
    /**
     * Mark message as read
     */
    suspend fun markMessageAsRead(conversationId: String, messageId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[SupabaseConfig.MESSAGES_TABLE]
                .update({
                    set("is_read", true)
                }) {
                    filter {
                        eq("message_id", messageId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest[SupabaseConfig.MESSAGES_TABLE]
                .update({
                    set("is_deleted", true)
                }) {
                    filter {
                        eq("message_id", messageId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
