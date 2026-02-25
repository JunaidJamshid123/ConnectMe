package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.Call
import com.junaidjamshid.i211203.domain.model.CallType
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for call-related operations.
 */
interface CallRepository {
    
    suspend fun initiateCall(
        callerId: String,
        receiverId: String,
        callType: CallType
    ): Resource<Call>
    
    suspend fun acceptCall(callId: String): Resource<Call>
    
    suspend fun rejectCall(callId: String): Resource<Unit>
    
    suspend fun endCall(callId: String): Resource<Unit>
    
    fun observeIncomingCalls(userId: String): Flow<Call?>
    
    suspend fun getCallHistory(userId: String): Resource<List<Call>>
    
    suspend fun getAgoraToken(channelName: String, userId: String): Resource<String>
}
