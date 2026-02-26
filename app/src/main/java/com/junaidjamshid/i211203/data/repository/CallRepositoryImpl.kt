package com.junaidjamshid.i211203.data.repository

import com.junaidjamshid.i211203.data.mapper.CallMapper
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseCallDataSource
import com.junaidjamshid.i211203.domain.model.Call
import com.junaidjamshid.i211203.domain.model.CallType
import com.junaidjamshid.i211203.domain.repository.CallRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CallRepository using Supabase.
 */
@Singleton
class CallRepositoryImpl @Inject constructor(
    private val callDataSource: SupabaseCallDataSource,
    private val callMapper: CallMapper
) : CallRepository {
    
    override suspend fun getCallHistory(userId: String): Resource<List<Call>> {
        return try {
            val callDtos = callDataSource.getCallHistory().first()
            Resource.Success(callDtos.map { callMapper.toDomain(it) })
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load call history")
        }
    }
    
    override fun observeIncomingCalls(userId: String): Flow<Call?> {
        return callDataSource.listenForIncomingCalls()
            .map { callDto ->
                callDto?.let { callMapper.toDomain(it) }
            }
    }
    
    override suspend fun initiateCall(
        callerId: String,
        receiverId: String,
        callType: CallType
    ): Resource<Call> {
        return callDataSource.initiateCall(
            receiverId = receiverId,
            receiverName = "",
            receiverProfileImage = "",
            callType = callType.name.lowercase()
        ).fold(
            onSuccess = { callDto ->
                Resource.Success(callMapper.toDomain(callDto))
            },
            onFailure = { e ->
                Resource.Error(e.message ?: "Failed to initiate call")
            }
        )
    }
    
    override suspend fun acceptCall(callId: String): Resource<Call> {
        return callDataSource.acceptCall(callId).fold(
            onSuccess = {
                // Return a minimal Call object since acceptCall only returns Unit
                Resource.Success(Call(callId = callId))
            },
            onFailure = { e -> Resource.Error(e.message ?: "Failed to accept call") }
        )
    }
    
    override suspend fun rejectCall(callId: String): Resource<Unit> {
        return callDataSource.rejectCall(callId).fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { e -> Resource.Error(e.message ?: "Failed to reject call") }
        )
    }
    
    override suspend fun endCall(callId: String): Resource<Unit> {
        return callDataSource.endCall(callId).fold(
            onSuccess = { Resource.Success(Unit) },
            onFailure = { e -> Resource.Error(e.message ?: "Failed to end call") }
        )
    }
    
    override suspend fun getAgoraToken(channelName: String, userId: String): Resource<String> {
        return try {
            // For now, return empty string - actual token should come from server
            Resource.Success("")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get Agora token")
        }
    }
}
