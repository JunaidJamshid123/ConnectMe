package com.junaidjamshid.i211203.data.mapper

import com.junaidjamshid.i211203.data.dto.MessageDto
import com.junaidjamshid.i211203.domain.model.Message
import com.junaidjamshid.i211203.domain.model.MessageType

/**
 * Mapper functions for Message data conversion.
 */
object MessageMapper {
    
    fun MessageDto.toDomain(): Message {
        return Message(
            messageId = messageId,
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            imageUrl = imageUrl,
            timestamp = timestamp,
            isRead = isRead,
            isDeleted = isDeleted,
            messageType = MessageType.valueOf(messageType)
        )
    }
    
    fun Message.toDto(): MessageDto {
        return MessageDto(
            messageId = messageId,
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            imageUrl = imageUrl,
            timestamp = timestamp,
            isRead = isRead,
            isDeleted = isDeleted,
            messageType = messageType.name
        )
    }
}
