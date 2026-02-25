package com.junaidjamshid.i211203.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.data.dto.MessageDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase data source for message-related operations.
 */
@Singleton
class FirebaseMessageDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    private val messagesRef = database.reference.child("Messages")
    private val conversationsRef = database.reference.child("Conversations")
    
    /**
     * Get conversations for current user as a Flow
     */
    fun getConversations(): Flow<List<MessageDto>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = mutableListOf<MessageDto>()
                
                for (conversationSnapshot in snapshot.children) {
                    // Get the last message from each conversation
                    val lastMessageSnapshot = conversationSnapshot.children.lastOrNull()
                    lastMessageSnapshot?.let { msgSnapshot ->
                        val message = parseMessageSnapshot(msgSnapshot)
                        if (message != null) {
                            conversations.add(message)
                        }
                    }
                }
                
                trySend(conversations.sortedByDescending { it.timestamp })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        messagesRef.addValueEventListener(listener)
        
        awaitClose { messagesRef.removeEventListener(listener) }
    }
    
    /**
     * Get messages for a specific conversation as a Flow
     */
    fun getMessages(conversationId: String): Flow<List<MessageDto>> = callbackFlow {
        val conversationRef = messagesRef.child(conversationId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<MessageDto>()
                
                for (messageSnapshot in snapshot.children) {
                    val message = parseMessageSnapshot(messageSnapshot)
                    if (message != null) {
                        messages.add(message)
                    }
                }
                
                trySend(messages.sortedBy { it.timestamp })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        conversationRef.addValueEventListener(listener)
        
        awaitClose { conversationRef.removeEventListener(listener) }
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
            val currentUserId = auth.currentUser?.uid 
                ?: return Result.failure(Exception("User not logged in"))
            
            val conversationId = getConversationId(currentUserId, receiverId)
            val messageId = messagesRef.child(conversationId).push().key
                ?: return Result.failure(Exception("Failed to generate message ID"))
            
            val messageDto = MessageDto(
                messageId = messageId,
                conversationId = conversationId,
                senderId = currentUserId,
                receiverId = receiverId,
                content = messageText,
                imageUrl = mediaUrl,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                isDeleted = false,
                messageType = messageType.uppercase()
            )
            
            messagesRef.child(conversationId).child(messageId).setValue(messageDto).await()
            
            // Update conversation metadata
            updateConversationMetadata(conversationId, currentUserId, receiverId, messageText)
            
            Result.success(messageDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark message as read
     */
    suspend fun markMessageAsRead(conversationId: String, messageId: String): Result<Unit> {
        return try {
            messagesRef.child(conversationId).child(messageId).child("isRead").setValue(true).await()
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
            messagesRef.child(conversationId).child(messageId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getConversationId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }
    
    private suspend fun updateConversationMetadata(
        conversationId: String,
        senderId: String,
        receiverId: String,
        lastMessage: String
    ) {
        val metadata = mapOf(
            "lastMessage" to lastMessage,
            "lastMessageTimestamp" to System.currentTimeMillis(),
            "participants" to listOf(senderId, receiverId)
        )
        
        conversationsRef.child(conversationId).updateChildren(metadata).await()
    }
    
    private fun parseMessageSnapshot(snapshot: DataSnapshot): MessageDto? {
        return try {
            MessageDto(
                messageId = snapshot.child("messageId").getValue(String::class.java) ?: snapshot.key ?: "",
                conversationId = snapshot.child("conversationId").getValue(String::class.java) ?: "",
                senderId = snapshot.child("senderId").getValue(String::class.java) ?: "",
                receiverId = snapshot.child("receiverId").getValue(String::class.java) ?: "",
                content = snapshot.child("content").getValue(String::class.java) ?: "",
                messageType = snapshot.child("messageType").getValue(String::class.java) ?: "TEXT",
                imageUrl = snapshot.child("imageUrl").getValue(String::class.java),
                timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                isRead = snapshot.child("isRead").getValue(Boolean::class.java) ?: false,
                isDeleted = snapshot.child("isDeleted").getValue(Boolean::class.java) ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
}
