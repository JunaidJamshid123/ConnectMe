package com.junaidjamshid.i211203.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.data.dto.CallDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase data source for call-related operations.
 */
@Singleton
class FirebaseCallDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    private val callsRef = database.reference.child("Calls")
    private val activeCallsRef = database.reference.child("ActiveCalls")
    
    /**
     * Get call history for current user
     */
    fun getCallHistory(): Flow<List<CallDto>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calls = mutableListOf<CallDto>()
                
                for (callSnapshot in snapshot.children) {
                    val call = parseCallSnapshot(callSnapshot)
                    if (call != null && (call.callerId == currentUserId || call.receiverId == currentUserId)) {
                        calls.add(call)
                    }
                }
                
                trySend(calls.sortedByDescending { it.startTimestamp })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        callsRef.addValueEventListener(listener)
        
        awaitClose { callsRef.removeEventListener(listener) }
    }
    
    /**
     * Listen for incoming calls
     */
    fun listenForIncomingCalls(): Flow<CallDto?> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (callSnapshot in snapshot.children) {
                    val call = parseCallSnapshot(callSnapshot)
                    if (call != null && call.receiverId == currentUserId && call.callStatus == "pending") {
                        trySend(call)
                        return
                    }
                }
                trySend(null)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        activeCallsRef.addValueEventListener(listener)
        
        awaitClose { activeCallsRef.removeEventListener(listener) }
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
            val currentUser = auth.currentUser 
                ?: return Result.failure(Exception("User not logged in"))
            
            val callId = UUID.randomUUID().toString()
            val channelName = "call_${callId}"
            
            val callDto = CallDto(
                callId = callId,
                callerId = currentUser.uid,
                callerName = currentUser.displayName ?: "Unknown",
                callerProfileImage = currentUser.photoUrl?.toString() ?: "",
                receiverId = receiverId,
                receiverName = receiverName,
                receiverProfileImage = receiverProfileImage,
                callType = callType,
                callStatus = "pending",
                channelName = channelName,
                agoraToken = "", // Token should be generated server-side
                startTimestamp = System.currentTimeMillis()
            )
            
            // Save to active calls
            activeCallsRef.child(callId).setValue(callDto).await()
            
            // Save to call history
            callsRef.child(callId).setValue(callDto).await()
            
            Result.success(callDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Accept a call
     */
    suspend fun acceptCall(callId: String): Result<Unit> {
        return try {
            activeCallsRef.child(callId).child("callStatus").setValue("accepted").await()
            callsRef.child(callId).child("callStatus").setValue("accepted").await()
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
            activeCallsRef.child(callId).child("callStatus").setValue("rejected").await()
            callsRef.child(callId).child("callStatus").setValue("rejected").await()
            activeCallsRef.child(callId).removeValue().await()
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
            val callSnapshot = callsRef.child(callId).get().await()
            val startTimestamp = callSnapshot.child("startTimestamp").getValue(Long::class.java) ?: 0L
            val duration = endTimestamp - startTimestamp
            
            val updates = mapOf(
                "callStatus" to "ended",
                "endTimestamp" to endTimestamp,
                "duration" to duration
            )
            
            callsRef.child(callId).updateChildren(updates).await()
            activeCallsRef.child(callId).removeValue().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseCallSnapshot(snapshot: DataSnapshot): CallDto? {
        return try {
            CallDto(
                callId = snapshot.child("callId").getValue(String::class.java) ?: snapshot.key ?: "",
                callerId = snapshot.child("callerId").getValue(String::class.java) ?: "",
                callerName = snapshot.child("callerName").getValue(String::class.java) ?: "",
                callerProfileImage = snapshot.child("callerProfileImage").getValue(String::class.java) ?: "",
                receiverId = snapshot.child("receiverId").getValue(String::class.java) ?: "",
                receiverName = snapshot.child("receiverName").getValue(String::class.java) ?: "",
                receiverProfileImage = snapshot.child("receiverProfileImage").getValue(String::class.java) ?: "",
                callType = snapshot.child("callType").getValue(String::class.java) ?: "voice",
                callStatus = snapshot.child("callStatus").getValue(String::class.java) ?: "pending",
                channelName = snapshot.child("channelName").getValue(String::class.java) ?: "",
                agoraToken = snapshot.child("agoraToken").getValue(String::class.java) ?: "",
                startTimestamp = snapshot.child("startTimestamp").getValue(Long::class.java) ?: 0L,
                endTimestamp = snapshot.child("endTimestamp").getValue(Long::class.java),
                duration = snapshot.child("duration").getValue(Long::class.java)
            )
        } catch (e: Exception) {
            null
        }
    }
}
