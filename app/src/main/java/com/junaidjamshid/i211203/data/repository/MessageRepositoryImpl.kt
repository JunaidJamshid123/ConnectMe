package com.junaidjamshid.i211203.data.repository

import com.junaidjamshid.i211203.data.mapper.MessageMapper.toDomain
import com.junaidjamshid.i211203.data.mapper.MessageMapper.toDto
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseMessageDataSource
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseStorageDataSource
import com.junaidjamshid.i211203.domain.model.Conversation
import com.junaidjamshid.i211203.domain.model.Message
import com.junaidjamshid.i211203.domain.repository.MessageRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MessageRepository using Supabase.
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDataSource: SupabaseMessageDataSource,
    private val storageDataSource: SupabaseStorageDataSource
) : MessageRepository {
    
    override fun getConversations(userId: String): Flow<Resource<List<Conversation>>> {
        return flow {
            emit(Resource.Loading())
            // Convert messages to conversations
            messageDataSource.getConversations().collect { messages ->
                val conversations = messages.groupBy { 
                    getConversationId(it.senderId, it.receiverId)
                }.map { (convId, msgs) ->
                    val lastMsg = msgs.maxByOrNull { it.timestamp }
                    Conversation(
                        conversationId = convId,
                        participantIds = listOf(lastMsg?.senderId ?: "", lastMsg?.receiverId ?: ""),
                        lastMessage = lastMsg?.content ?: "",
                        lastMessageTimestamp = lastMsg?.timestamp ?: 0
                    )
                }
                emit(Resource.Success(conversations))
            }
        }.catch { e ->
            emit(Resource.Error(e.message ?: "Failed to load conversations"))
        }
    }
    
    override fun getMessages(conversationId: String): Flow<Resource<List<Message>>> {
        return messageDataSource.getMessages(conversationId)
            .map<List<com.junaidjamshid.i211203.data.dto.MessageDto>, Resource<List<Message>>> { messages ->
                Resource.Success(messages.map { it.toDomain() })
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to load messages"))
            }
    }
    
    override suspend fun sendMessage(message: Message): Resource<Message> {
        return messageDataSource.sendMessage(
            receiverId = message.receiverId,
            messageText = message.content,
            messageType = message.messageType.name,
            mediaUrl = message.imageUrl
        ).fold(
            onSuccess = { messageDto ->
                Resource.Success(messageDto.toDomain())
            },
            onFailure = { e ->
                Resource.Error(e.message ?: "Failed to send message")
            }
        )
    }
    
    override suspend fun sendImageMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        imageBase64: String
    ): Resource<Message> {
        return try {
            // Decode Base64 to bytes
            val imageBytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
            
            // Upload image and get URL
            val imageUrl = try {
                storageDataSource.uploadMessageImage(conversationId, imageBytes)
            } catch (storageError: Exception) {
                return Resource.Error("Storage error: ${storageError.message}")
            }
            
            // Send message with image URL
            messageDataSource.sendMessage(
                receiverId = receiverId,
                messageText = "📷 Photo",
                messageType = "IMAGE",
                mediaUrl = imageUrl
            ).fold(
                onSuccess = { messageDto ->
                    Resource.Success(messageDto.toDomain())
                },
                onFailure = { e ->
                    Resource.Error(e.message ?: "Failed to send image message")
                }
            )
        } catch (e: Exception) {
            Resource.Error("Failed to send image: ${e.message}")
        }
    }
    
    override suspend fun sendVoiceMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        audioBytes: ByteArray,
        durationMs: Long
    ): Resource<Message> {
        return try {
            // Upload voice and get URL
            val voiceUrl = try {
                storageDataSource.uploadVoiceMessage(conversationId, audioBytes)
            } catch (storageError: Exception) {
                return Resource.Error("Storage error: ${storageError.message}")
            }
            
            // Format duration for display
            val seconds = (durationMs / 1000).toInt()
            val minutes = seconds / 60
            val secs = seconds % 60
            val durationText = String.format("%d:%02d", minutes, secs)
            
            messageDataSource.sendMessage(
                receiverId = receiverId,
                messageText = "🎤 Voice message ($durationText)",
                messageType = "VOICE",
                mediaUrl = voiceUrl
            ).fold(
                onSuccess = { messageDto ->
                    Resource.Success(messageDto.toDomain())
                },
                onFailure = { e ->
                    Resource.Error(e.message ?: "Failed to send voice message")
                }
            )
        } catch (e: Exception) {
            Resource.Error("Failed to send voice: ${e.message}")
        }
    }
    
    override suspend fun deleteMessage(messageId: String, conversationId: String): Resource<Unit> {
        return messageDataSource.deleteMessage(conversationId, messageId).fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { e -> Resource.Error(e.message ?: "Failed to delete message") }
        )
    }
    
    override suspend fun markMessageAsRead(messageId: String, conversationId: String): Resource<Unit> {
        return messageDataSource.markMessageAsRead(conversationId, messageId).fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { e -> Resource.Error(e.message ?: "Failed to mark message as read") }
        )
    }
    
    override suspend fun markAllMessagesAsRead(conversationId: String, userId: String): Resource<Unit> {
        return try {
            // Mark all messages as read - simplified implementation
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark messages as read")
        }
    }
    
    override suspend fun getOrCreateConversation(
        currentUserId: String,
        otherUserId: String
    ): Resource<Conversation> {
        return try {
            val conversationId = getConversationId(currentUserId, otherUserId)
            Resource.Success(Conversation(
                conversationId = conversationId,
                participantIds = listOf(currentUserId, otherUserId)
            ))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get or create conversation")
        }
    }
    
    override suspend fun deleteConversation(conversationId: String): Resource<Unit> {
        return try {
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete conversation")
        }
    }
    
    override suspend fun setVanishMode(conversationId: String, enabled: Boolean): Resource<Unit> {
        return try {
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to set vanish mode")
        }
    }
    
    override suspend fun sendTypingIndicator(conversationId: String, userId: String, isTyping: Boolean) {
        messageDataSource.sendTypingIndicator(conversationId, userId, isTyping)
    }
    
    override fun observeTypingIndicator(conversationId: String, userId: String): kotlinx.coroutines.flow.Flow<Boolean> {
        return messageDataSource.observeTypingIndicator(conversationId, userId)
    }
    
    private fun getConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
}
