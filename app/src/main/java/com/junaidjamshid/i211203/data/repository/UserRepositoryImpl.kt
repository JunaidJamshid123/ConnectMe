package com.junaidjamshid.i211203.data.repository

import com.junaidjamshid.i211203.data.mapper.UserMapper.toDomain
import com.junaidjamshid.i211203.data.mapper.UserMapper.toUpdateMap
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseUserDataSource
import com.junaidjamshid.i211203.domain.model.User
import com.junaidjamshid.i211203.domain.repository.UserRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository that uses Supabase.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDataSource: SupabaseUserDataSource
) : UserRepository {
    
    override suspend fun getUserById(userId: String): Resource<User> {
        return try {
            val userDto = userDataSource.getUserById(userId)
            if (userDto != null) {
                Resource.Success(userDto.toDomain())
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user")
        }
    }
    
    override fun getUserByIdFlow(userId: String): Flow<Resource<User>> {
        return userDataSource.getUserByIdFlow(userId).map { userDto ->
            if (userDto != null) {
                Resource.Success(userDto.toDomain())
            } else {
                Resource.Error("User not found")
            }
        }
    }
    
    override suspend fun updateUserProfile(user: User): Resource<Unit> {
        return try {
            userDataSource.updateUserProfile(user.userId, user.toUpdateMap())
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }
    
    override suspend fun updateUserProfile(
        fullName: String,
        username: String,
        phone: String,
        bio: String,
        profileImage: ByteArray?
    ): Resource<Unit> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId() 
                ?: return Resource.Error("User not logged in")
            
            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName,
                "username" to username,
                "phoneNumber" to phone,
                "bio" to bio
            )
            
            // Handle profile image if provided
            if (profileImage != null) {
                val base64Image = android.util.Base64.encodeToString(profileImage, android.util.Base64.DEFAULT)
                updates["profilePicture"] = base64Image
            }
            
            userDataSource.updateUserProfile(currentUserId, updates)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }
    
    override suspend fun updateProfilePicture(userId: String, imageBase64: String): Resource<String> {
        return try {
            userDataSource.updateUserProfile(userId, mapOf("profilePicture" to imageBase64))
            Resource.Success(imageBase64)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile picture")
        }
    }
    
    override suspend fun updateCoverPhoto(userId: String, imageBase64: String): Resource<String> {
        return try {
            userDataSource.updateUserProfile(userId, mapOf("coverPhoto" to imageBase64))
            Resource.Success(imageBase64)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update cover photo")
        }
    }
    
    override suspend fun searchUsers(query: String): Resource<List<User>> {
        return try {
            val users = userDataSource.searchUsers(query).map { it.toDomain() }
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Search failed")
        }
    }
    
    override suspend fun followUser(currentUserId: String, targetUserId: String): Resource<Unit> {
        return try {
            userDataSource.followUser(currentUserId, targetUserId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to follow user")
        }
    }
    
    override suspend fun unfollowUser(currentUserId: String, targetUserId: String): Resource<Unit> {
        return try {
            userDataSource.unfollowUser(currentUserId, targetUserId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unfollow user")
        }
    }
    
    override fun getFollowers(userId: String): Flow<Resource<List<User>>> {
        return flow {
            emit(Resource.Loading())
            try {
                val followerIds = userDataSource.getFollowers(userId)
                val followers = followerIds.mapNotNull { id ->
                    userDataSource.getUserById(id)?.toDomain()
                }
                emit(Resource.Success(followers))
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Failed to get followers"))
            }
        }
    }
    
    override fun getFollowing(userId: String): Flow<Resource<List<User>>> {
        return flow {
            emit(Resource.Loading())
            try {
                val followingIds = userDataSource.getFollowing(userId)
                val following = followingIds.mapNotNull { id ->
                    userDataSource.getUserById(id)?.toDomain()
                }
                emit(Resource.Success(following))
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Failed to get following"))
            }
        }
    }
    
    override fun isFollowing(currentUserId: String, targetUserId: String): Flow<Resource<Boolean>> {
        return flow {
            emit(Resource.Loading())
            try {
                val isFollowing = userDataSource.isFollowing(currentUserId, targetUserId)
                emit(Resource.Success(isFollowing))
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Failed to check following status"))
            }
        }
    }
    
    override suspend fun blockUser(currentUserId: String, targetUserId: String): Resource<Unit> {
        return try {
            userDataSource.updateUserProfile(
                currentUserId,
                mapOf("blockedUsers/$targetUserId" to true)
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to block user")
        }
    }
    
    override suspend fun unblockUser(currentUserId: String, targetUserId: String): Resource<Unit> {
        return try {
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unblock user")
        }
    }
    
    override suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Resource<Unit> {
        return try {
            userDataSource.updateOnlineStatus(userId, isOnline)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update online status")
        }
    }
    
    override suspend fun updateLastSeen(userId: String): Resource<Unit> {
        return try {
            userDataSource.updateUserProfile(userId, mapOf("lastSeen" to System.currentTimeMillis()))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update last seen")
        }
    }
    
    override fun getUserProfile(userId: String): Flow<Resource<User>> {
        return getUserByIdFlow(userId)
    }
    
    override fun getAllUsers(): Flow<Resource<List<User>>> {
        return userDataSource.getAllUsersFlow().map { users ->
            Resource.Success(users.map { it.toDomain() })
        }
    }
    
    override suspend fun followUser(targetUserId: String): Resource<Unit> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                userDataSource.followUser(currentUserId, targetUserId)
                Resource.Success(Unit)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to follow user")
        }
    }
    
    override suspend fun unfollowUser(targetUserId: String): Resource<Unit> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                userDataSource.unfollowUser(currentUserId, targetUserId)
                Resource.Success(Unit)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unfollow user")
        }
    }
    
    override suspend fun removeFollower(userId: String): Resource<Unit> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                userDataSource.unfollowUser(userId, currentUserId)
                Resource.Success(Unit)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove follower")
        }
    }
    
    override fun getRecentSearches(): Flow<Resource<List<User>>> {
        return userDataSource.getRecentSearchesFlow().map { searchedUserIds ->
            try {
                val users = searchedUserIds.mapNotNull { id ->
                    userDataSource.getUserById(id)?.toDomain()
                }
                Resource.Success(users)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to get recent searches")
            }
        }
    }
    
    override suspend fun saveRecentSearch(userId: String) {
        try {
            userDataSource.saveRecentSearch(userId)
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
    
    override suspend fun removeRecentSearch(userId: String) {
        try {
            userDataSource.removeRecentSearch(userId)
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
    
    override suspend fun clearAllRecentSearches() {
        try {
            userDataSource.clearAllRecentSearches()
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }
}
