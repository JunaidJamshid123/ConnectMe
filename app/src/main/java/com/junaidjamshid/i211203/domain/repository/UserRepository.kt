package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user-related operations.
 */
interface UserRepository {
    
    suspend fun getUserById(userId: String): Resource<User>
    
    fun getUserByIdFlow(userId: String): Flow<Resource<User>>
    
    fun getUserProfile(userId: String): Flow<Resource<User>>
    
    fun getAllUsers(): Flow<Resource<List<User>>>
    
    suspend fun updateUserProfile(user: User): Resource<Unit>
    
    suspend fun updateUserProfile(
        fullName: String,
        username: String,
        phone: String,
        bio: String,
        profileImage: ByteArray?
    ): Resource<Unit>
    
    suspend fun updateProfilePicture(userId: String, imageBase64: String): Resource<String>
    
    suspend fun updateCoverPhoto(userId: String, imageBase64: String): Resource<String>
    
    suspend fun searchUsers(query: String): Resource<List<User>>
    
    suspend fun followUser(targetUserId: String): Resource<Unit>
    
    suspend fun unfollowUser(targetUserId: String): Resource<Unit>
    
    suspend fun followUser(currentUserId: String, targetUserId: String): Resource<Unit>
    
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Resource<Unit>
    
    fun getFollowers(userId: String): Flow<Resource<List<User>>>
    
    fun getFollowing(userId: String): Flow<Resource<List<User>>>
    
    fun isFollowing(currentUserId: String, targetUserId: String): Flow<Resource<Boolean>>
    
    suspend fun removeFollower(userId: String): Resource<Unit>
    
    suspend fun blockUser(currentUserId: String, targetUserId: String): Resource<Unit>
    
    suspend fun unblockUser(currentUserId: String, targetUserId: String): Resource<Unit>
    
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Resource<Unit>
    
    suspend fun updateLastSeen(userId: String): Resource<Unit>
    
    // Recent searches
    fun getRecentSearches(): Flow<Resource<List<User>>>
    
    suspend fun saveRecentSearch(userId: String)
    
    suspend fun removeRecentSearch(userId: String)
    
    suspend fun clearAllRecentSearches()
}
