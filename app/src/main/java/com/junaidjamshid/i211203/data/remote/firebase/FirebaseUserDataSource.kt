package com.junaidjamshid.i211203.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.data.dto.UserDto
import com.junaidjamshid.i211203.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firebase User operations.
 */
@Singleton
class FirebaseUserDataSource @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val firebaseAuth: FirebaseAuth
) {
    
    private val usersRef = firebaseDatabase.reference.child(Constants.USERS_REF)
    
    fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
    
    suspend fun getUserById(userId: String): UserDto? {
        val snapshot = usersRef.child(userId).get().await()
        return snapshot.getValue(UserDto::class.java)
    }
    
    fun getUserByIdFlow(userId: String): Flow<UserDto?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(UserDto::class.java)
                trySend(user)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        usersRef.child(userId).addValueEventListener(listener)
        
        awaitClose {
            usersRef.child(userId).removeEventListener(listener)
        }
    }
    
    fun getAllUsersFlow(): Flow<List<UserDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { it.getValue(UserDto::class.java) }
                trySend(users)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        usersRef.addValueEventListener(listener)
        
        awaitClose {
            usersRef.removeEventListener(listener)
        }
    }
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        usersRef.child(userId).updateChildren(updates).await()
    }
    
    suspend fun searchUsers(query: String): List<UserDto> {
        val queryLower = query.lowercase()
        val snapshot = usersRef.orderByChild("username")
            .startAt(queryLower)
            .endAt(queryLower + "\uf8ff")
            .limitToFirst(20)
            .get()
            .await()
        
        return snapshot.children.mapNotNull { it.getValue(UserDto::class.java) }
    }
    
    suspend fun followUser(currentUserId: String, targetUserId: String) {
        // Add to current user's following
        usersRef.child(currentUserId)
            .child("following")
            .child(targetUserId)
            .setValue(true)
            .await()
        
        // Add to target user's followers
        usersRef.child(targetUserId)
            .child("followers")
            .child(currentUserId)
            .setValue(true)
            .await()
    }
    
    suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        // Remove from current user's following
        usersRef.child(currentUserId)
            .child("following")
            .child(targetUserId)
            .removeValue()
            .await()
        
        // Remove from target user's followers
        usersRef.child(targetUserId)
            .child("followers")
            .child(currentUserId)
            .removeValue()
            .await()
    }
    
    suspend fun getFollowers(userId: String): List<String> {
        val snapshot = usersRef.child(userId).child("followers").get().await()
        return snapshot.children.mapNotNull { it.key }
    }
    
    suspend fun getFollowing(userId: String): List<String> {
        val snapshot = usersRef.child(userId).child("following").get().await()
        return snapshot.children.mapNotNull { it.key }
    }
    
    fun getFollowersFlow(userId: String): Flow<List<String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followerIds = snapshot.children.mapNotNull { it.key }
                trySend(followerIds)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        usersRef.child(userId).child("followers").addValueEventListener(listener)
        
        awaitClose {
            usersRef.child(userId).child("followers").removeEventListener(listener)
        }
    }
    
    fun getFollowingFlow(userId: String): Flow<List<String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingIds = snapshot.children.mapNotNull { it.key }
                trySend(followingIds)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        usersRef.child(userId).child("following").addValueEventListener(listener)
        
        awaitClose {
            usersRef.child(userId).child("following").removeEventListener(listener)
        }
    }
    
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        val snapshot = usersRef.child(currentUserId)
            .child("following")
            .child(targetUserId)
            .get()
            .await()
        return snapshot.exists()
    }
    
    fun isFollowingFlow(currentUserId: String, targetUserId: String): Flow<Boolean> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        usersRef.child(currentUserId)
            .child("following")
            .child(targetUserId)
            .addValueEventListener(listener)
        
        awaitClose {
            usersRef.child(currentUserId)
                .child("following")
                .child(targetUserId)
                .removeEventListener(listener)
        }
    }
    
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        val updates = mapOf(
            "onlineStatus" to isOnline,
            "lastSeen" to System.currentTimeMillis()
        )
        usersRef.child(userId).updateChildren(updates).await()
    }
    
    // Recent searches methods
    private fun getRecentSearchesRef(userId: String) = 
        firebaseDatabase.reference.child("recent_searches").child(userId)
    
    fun getRecentSearchesFlow(): Flow<List<String>> = callbackFlow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val searchedUserIds = snapshot.children
                    .sortedByDescending { it.child("timestamp").getValue(Long::class.java) ?: 0L }
                    .mapNotNull { it.key }
                trySend(searchedUserIds)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        getRecentSearchesRef(currentUserId).addValueEventListener(listener)
        
        awaitClose {
            getRecentSearchesRef(currentUserId).removeEventListener(listener)
        }
    }
    
    suspend fun saveRecentSearch(searchedUserId: String) {
        val currentUserId = getCurrentUserId() ?: return
        getRecentSearchesRef(currentUserId)
            .child(searchedUserId)
            .setValue(mapOf("timestamp" to System.currentTimeMillis()))
            .await()
    }
    
    suspend fun removeRecentSearch(searchedUserId: String) {
        val currentUserId = getCurrentUserId() ?: return
        getRecentSearchesRef(currentUserId)
            .child(searchedUserId)
            .removeValue()
            .await()
    }
    
    suspend fun clearAllRecentSearches() {
        val currentUserId = getCurrentUserId() ?: return
        getRecentSearchesRef(currentUserId).removeValue().await()
    }
}
