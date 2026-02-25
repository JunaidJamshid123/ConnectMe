package com.junaidjamshid.i211203.data.mapper

import com.junaidjamshid.i211203.data.dto.CallDto
import com.junaidjamshid.i211203.domain.model.Call
import com.junaidjamshid.i211203.domain.model.CallStatus
import com.junaidjamshid.i211203.domain.model.CallType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for Call domain model and CallDto.
 */
@Singleton
class CallMapper @Inject constructor() {
    
    fun toDomain(dto: CallDto): Call {
        return Call(
            callId = dto.callId,
            callerId = dto.callerId,
            callerName = dto.callerName,
            callerProfileImage = dto.callerProfileImage,
            receiverId = dto.receiverId,
            receiverName = dto.receiverName,
            receiverProfileImage = dto.receiverProfileImage,
            callType = parseCallType(dto.callType),
            callStatus = parseCallStatus(dto.callStatus),
            channelName = dto.channelName,
            timestamp = dto.startTimestamp,
            duration = dto.duration ?: 0
        )
    }
    
    fun toDto(domain: Call): CallDto {
        return CallDto(
            callId = domain.callId,
            callerId = domain.callerId,
            callerName = domain.callerName,
            callerProfileImage = domain.callerProfileImage,
            receiverId = domain.receiverId,
            receiverName = domain.receiverName,
            receiverProfileImage = domain.receiverProfileImage,
            callType = domain.callType.name.lowercase(),
            callStatus = domain.callStatus.name.lowercase(),
            channelName = domain.channelName,
            startTimestamp = domain.timestamp,
            duration = domain.duration
        )
    }
    
    private fun parseCallType(type: String): CallType {
        return when (type.lowercase()) {
            "video" -> CallType.VIDEO
            "audio" -> CallType.AUDIO
            else -> CallType.VIDEO
        }
    }
    
    private fun parseCallStatus(status: String): CallStatus {
        return when (status.lowercase()) {
            "pending" -> CallStatus.PENDING
            "ongoing" -> CallStatus.ONGOING
            "ended" -> CallStatus.ENDED
            "missed" -> CallStatus.MISSED
            "rejected" -> CallStatus.REJECTED
            else -> CallStatus.PENDING
        }
    }
}
