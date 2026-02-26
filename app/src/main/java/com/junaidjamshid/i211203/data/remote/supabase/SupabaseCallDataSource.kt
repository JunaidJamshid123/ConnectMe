package com.junaidjamshid.i211203.data.remote.supabase

import com.junaidjamshid.i211203.data.dto.CallDto
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
 * Supabase representation of Call.
 */
@Serializable
data class SupabaseCall(
    val call_id: String = "",
    val caller_id: String = "",
    val caller_name: String = "",
    val caller_profile_image: String = "",
    val receiver_id: String = "",
    val receiver_name: String = "",
    val receiver_profile_image: String = "",
    val call_type: String = "voice",
    val call_status: String = "pending",
    val channel_name: String = "",
    val agora_token: String = "",
    val start_timestamp: Long = 0L,
    val end_timestamp: Long? = null,
    val duration: Long? = null
)

fun SupabaseCall.toDto(): CallDto = CallDto(
    callId = call_id,
    callerId = caller_id,
    callerName = caller_name,
    callerProfileImage = caller_profile_image,
    receiverId = receiver_id,
    receiverName = receiver_name,
    receiverProfileImage = receiver_profile_image,
    callType = call_type,
    callStatus = call_status,
    channelName = channel_name,
    agoraToken = agora_token,
    startTimestamp = start_timestamp,
    endTimestamp = end_timestamp,
    duration = duration
)

/**
 * Data source for Supabase Call operations.
 */
@Singleton
class SupabaseCallDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    private fun getCurrentUserId(): String? = supabaseClient.auth.currentUserOrNull()?.id
    
    /**
     * Get call history for current user
     */
    fun getCallHistory(): Flow<List<CallDto>> = flow {
        val currentUserId = getCurrentUserId() ?: return@flow
        
        emit(getCallHistoryList(currentUserId))
        
        val channel = supabaseClient.realtime.channel("call-history-$currentUserId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.CALLS_TABLE
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getCallHistoryList(currentUserId))
        }
    }
    
    private suspend fun getCallHistoryList(currentUserId: String): List<CallDto> {
        val calls = supabaseClient.postgrest[SupabaseConfig.CALLS_TABLE]
            .select {
                filter {
                    or {
                        eq("caller_id", currentUserId)
                        eq("receiver_id", currentUserId)
                    }
                }
                order("start_timestamp", Order.DESCENDING)
            }
            .decodeList<SupabaseCall>()
        return calls.map { it.toDto() }
    }
    
    /**
     * Listen for incoming calls
     */
    fun listenForIncomingCalls(): Flow<CallDto?> = flow {
        val currentUserId = getCurrentUserId() ?: return@flow
        
        emit(getPendingCall(currentUserId))
        
        val channel = supabaseClient.realtime.channel("incoming-calls-$currentUserId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.ACTIVE_CALLS_TABLE
            filter("receiver_id", FilterOperator.EQ, currentUserId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getPendingCall(currentUserId))
        }
    }
    
    private suspend fun getPendingCall(currentUserId: String): CallDto? {
        val call = supabaseClient.postgrest[SupabaseConfig.ACTIVE_CALLS_TABLE]
            .select {
                filter {
                    eq("receiver_id", currentUserId)
                    eq("call_status", "pending")
                }
            }
            .decodeSingleOrNull<SupabaseCall>()
        return call?.toDto()
    }
    
    /**
     * Initiate a call
     */
    suspend fun initiateCall(
        receiverId: String,
        receiverName: String,
        receiverProfileImage: String,
        callType: String
    ): Result<CallDto> {
        return try {
            val currentUserId = getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))
            
            val callId = UUID.randomUUID().toString()
            val channelName = "call_$callId"
            
            val supabaseCall = SupabaseCall(
                call_id = callId,
                caller_id = currentUserId,
                caller_name = "", // Will be filled by the app
                caller_profile_image = "",
                receiver_id = receiverId,
                receiver_name = receiverName,
                receiver_profile_image = receiverProfileImage,
                call_type = callType,
                call_status = "pending",
                channel_name = channelName,
                agora_token = "", // Token should be generated server-side
                start_timestamp = System.currentTimeMillis()
            )
            
            // Save to active calls
            supabaseClient.postgrest[SupabaseConfig.ACTIVE_CALLS_TABLE]
                .insert(supabaseCall)
            
            // Save to call history
            supabaseClient.postgrest[SupabaseConfig.CALLS_TABLE]
                .insert(supabaseCall)
            
            Result.success(supabaseCall.toDto())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Accept a call
     */
    suspend fun acceptCall(callId: String): Result<Unit> {
        return try {
            // Update active calls
            supabaseClient.postgrest[SupabaseConfig.ACTIVE_CALLS_TABLE]
                .update({
                    set("call_status", "accepted")
                }) {
                    filter {
                        eq("call_id", callId)
                    }
                }
            
            // Update call history
            supabaseClient.postgrest[SupabaseConfig.CALLS_TABLE]
                .update({
                    set("call_status", "accepted")
                }) {
                    filter {
                        eq("call_id", callId)
                    }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reject a call
     */
    suspend fun rejectCall(callId: String): Result<Unit> {
        return try {
            // Update call history
            supabaseClient.postgrest[SupabaseConfig.CALLS_TABLE]
                .update({
                    set("call_status", "rejected")
                }) {
                    filter {
                        eq("call_id", callId)
                    }
                }
            
            // Remove from active calls
            supabaseClient.postgrest[SupabaseConfig.ACTIVE_CALLS_TABLE]
                .delete {
                    filter {
                        eq("call_id", callId)
                    }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * End a call
     */
    suspend fun endCall(callId: String): Result<Unit> {
        return try {
            val endTimestamp = System.currentTimeMillis()
            
            // Get call to calculate duration
            val call = supabaseClient.postgrest[SupabaseConfig.CALLS_TABLE]
                .select {
                    filter {
                        eq("call_id", callId)
                    }
                }
                .decodeSingleOrNull<SupabaseCall>()
            
            val duration = call?.let { endTimestamp - it.start_timestamp } ?: 0L
            
            // Update call history
            supabaseClient.postgrest[SupabaseConfig.CALLS_TABLE]
                .update({
                    set("call_status", "ended")
                    set("end_timestamp", endTimestamp)
                    set("duration", duration)
                }) {
                    filter {
                        eq("call_id", callId)
                    }
                }
            
            // Remove from active calls
            supabaseClient.postgrest[SupabaseConfig.ACTIVE_CALLS_TABLE]
                .delete {
                    filter {
                        eq("call_id", callId)
                    }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
