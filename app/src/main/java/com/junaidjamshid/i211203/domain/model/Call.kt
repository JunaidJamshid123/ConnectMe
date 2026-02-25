package com.junaidjamshid.i211203.domain.model

/**
 * Domain model representing a Call in the application.
 */
data class Call(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerProfileImage: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverProfileImage: String = "",
    val channelName: String = "",
    val callType: CallType = CallType.VIDEO,
    val callStatus: CallStatus = CallStatus.PENDING,
    val timestamp: Long = 0,
    val duration: Long = 0
)

enum class CallType {
    VIDEO,
    AUDIO
}

enum class CallStatus {
    PENDING,
    ONGOING,
    ENDED,
    MISSED,
    REJECTED
}
