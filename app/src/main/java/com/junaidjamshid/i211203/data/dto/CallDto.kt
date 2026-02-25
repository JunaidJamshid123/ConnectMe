package com.junaidjamshid.i211203.data.dto

/**
 * Call Data Transfer Object for Firebase.
 */
data class CallDto(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerProfileImage: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverProfileImage: String = "",
    val callType: String = "voice", // "voice" or "video"
    val callStatus: String = "pending", // "pending", "accepted", "rejected", "ended", "missed"
    val channelName: String = "",
    val agoraToken: String = "",
    val startTimestamp: Long = 0L,
    val endTimestamp: Long? = null,
    val duration: Long? = null
)
