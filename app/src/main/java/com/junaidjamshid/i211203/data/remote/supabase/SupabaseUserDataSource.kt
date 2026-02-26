package com.junaidjamshid.i211203.data.remote.supabase

import com.junaidjamshid.i211203.data.dto.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase representation of User for database operations.
 */
@Serializable
data class SupabaseUser(
    val user_id: String = "",
    val username: String = "",
    val email: String = "",
    val full_name: String = "",
    val phone_number: String = "",
    val profile_picture: String? = null,
    val cover_photo: String? = null,
    val bio: String = "",
    val online_status: Boolean = false,
    val push_token: String = "",
    val created_at: Long = 0,
    val last_seen: Long = 0,
    val vanish_mode_enabled: Boolean = false
)

/**
 * Supabase representation of follower relationship.
 */
@Serializable
data class FollowerRelation(
    val id: Long? = null,
    val follower_id: String,
    val following_id: String,
    val created_at: Long = System.currentTimeMillis()
)

/**
 * Supabase representation of recent search.
 */
@Serializable
data class RecentSearch(
    val id: Long? = null,
    val user_id: String,
    val searched_user_id: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun SupabaseUser.toDto(): UserDto = UserDto(
    userId = user_id,
    username = username,
    email = email,
    fullName = full_name,
    phoneNumber = phone_number,
    profilePicture = profile_picture,
    coverPhoto = cover_photo,
    bio = bio,
    onlineStatus = online_status,
    pushToken = push_token,
    createdAt = created_at,
    lastSeen = last_seen,
    vanishModeEnabled = vanish_mode_enabled
)

/**
 * Data source for Supabase User operations.
 */
@Singleton
class SupabaseUserDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    fun getCurrentUserId(): String? = supabaseClient.auth.currentUserOrNull()?.id
    
    suspend fun getUserById(userId: String): UserDto? {
        val result = supabaseClient.postgrest[SupabaseConfig.USERS_TABLE]
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeSingleOrNull<SupabaseUser>()
        return result?.toDto()
    }
    
    fun getUserByIdFlow(userId: String): Flow<UserDto?> = flow {
        // Initial fetch
        emit(getUserById(userId))
        
        // Setup realtime subscription
        val channel = supabaseClient.realtime.channel("user-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.USERS_TABLE
            filter("user_id", FilterOperator.EQ, userId)
        }
        
        channel.subscribe()
        
        changeFlow.collect { action ->
            when (action) {
                is PostgresAction.Update -> {
                    val user = getUserById(userId)
                    emit(user)
                }
                is PostgresAction.Delete -> emit(null)
                else -> {}
            }
        }
    }
    
    suspend fun getAllUsers(): List<UserDto> {
        val result = supabaseClient.postgrest[SupabaseConfig.USERS_TABLE]
            .select()
            .decodeList<SupabaseUser>()
        return result.map { it.toDto() }
    }
    
    fun getAllUsersFlow(): Flow<List<UserDto>> = flow {
        // Initial fetch
        emit(getAllUsers())
        
        // Setup realtime subscription
        val channel = supabaseClient.realtime.channel("all-users")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.USERS_TABLE
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getAllUsers())
        }
    }
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        supabaseClient.postgrest[SupabaseConfig.USERS_TABLE]
            .update({
                updates.forEach { (key, value) ->
                    // Convert camelCase to snake_case
                    val snakeKey = key.replace(Regex("([a-z])([A-Z])")) { 
                        "${it.groupValues[1]}_${it.groupValues[2].lowercase()}" 
                    }
                    when (value) {
                        is String -> set(snakeKey, value)
                        is Boolean -> set(snakeKey, value)
                        is Long -> set(snakeKey, value)
                        is Int -> set(snakeKey, value)
                    }
                }
            }) {
                filter {
                    eq("user_id", userId)
                }
            }
    }
    
    suspend fun searchUsers(query: String): List<UserDto> {
        val result = supabaseClient.postgrest[SupabaseConfig.USERS_TABLE]
            .select {
                filter {
                    ilike("username", "%$query%")
                }
                limit(20)
            }
            .decodeList<SupabaseUser>()
        return result.map { it.toDto() }
    }
    
    suspend fun followUser(currentUserId: String, targetUserId: String) {
        val relation = FollowerRelation(
            follower_id = currentUserId,
            following_id = targetUserId
        )
        supabaseClient.postgrest[SupabaseConfig.FOLLOWERS_TABLE]
            .insert(relation)
    }
    
    suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        supabaseClient.postgrest[SupabaseConfig.FOLLOWERS_TABLE]
            .delete {
                filter {
                    eq("follower_id", currentUserId)
                    eq("following_id", targetUserId)
                }
            }
    }
    
    suspend fun getFollowers(userId: String): List<String> {
        val result = supabaseClient.postgrest[SupabaseConfig.FOLLOWERS_TABLE]
            .select(columns = Columns.list("follower_id")) {
                filter {
                    eq("following_id", userId)
                }
            }
            .decodeList<Map<String, String>>()
        return result.mapNotNull { it["follower_id"] }
    }
    
    suspend fun getFollowing(userId: String): List<String> {
        val result = supabaseClient.postgrest[SupabaseConfig.FOLLOWERS_TABLE]
            .select(columns = Columns.list("following_id")) {
                filter {
                    eq("follower_id", userId)
                }
            }
            .decodeList<Map<String, String>>()
        return result.mapNotNull { it["following_id"] }
    }
    
    fun getFollowersFlow(userId: String): Flow<List<String>> = flow {
        emit(getFollowers(userId))
        
        val channel = supabaseClient.realtime.channel("followers-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.FOLLOWERS_TABLE
            filter("following_id", FilterOperator.EQ, userId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getFollowers(userId))
        }
    }
    
    fun getFollowingFlow(userId: String): Flow<List<String>> = flow {
        emit(getFollowing(userId))
        
        val channel = supabaseClient.realtime.channel("following-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.FOLLOWERS_TABLE
            filter("follower_id", FilterOperator.EQ, userId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getFollowing(userId))
        }
    }
    
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        val result = supabaseClient.postgrest[SupabaseConfig.FOLLOWERS_TABLE]
            .select {
                filter {
                    eq("follower_id", currentUserId)
                    eq("following_id", targetUserId)
                }
            }
            .decodeList<FollowerRelation>()
        return result.isNotEmpty()
    }
    
    fun isFollowingFlow(currentUserId: String, targetUserId: String): Flow<Boolean> = flow {
        emit(isFollowing(currentUserId, targetUserId))
        
        val channel = supabaseClient.realtime.channel("is-following-$currentUserId-$targetUserId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.FOLLOWERS_TABLE
            filter("follower_id", FilterOperator.EQ, currentUserId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(isFollowing(currentUserId, targetUserId))
        }
    }
    
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        supabaseClient.postgrest[SupabaseConfig.USERS_TABLE]
            .update({
                set("online_status", isOnline)
                set("last_seen", System.currentTimeMillis())
            }) {
                filter {
                    eq("user_id", userId)
                }
            }
    }
    
    // Recent searches methods
    fun getRecentSearchesFlow(): Flow<List<String>> = flow {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            emit(emptyList())
            return@flow
        }
        
        emit(getRecentSearches(currentUserId))
        
        val channel = supabaseClient.realtime.channel("recent-searches-$currentUserId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.RECENT_SEARCHES_TABLE
            filter("user_id", FilterOperator.EQ, currentUserId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getRecentSearches(currentUserId))
        }
    }
    
    private suspend fun getRecentSearches(userId: String): List<String> {
        val result = supabaseClient.postgrest[SupabaseConfig.RECENT_SEARCHES_TABLE]
            .select {
                filter {
                    eq("user_id", userId)
                }
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<RecentSearch>()
        return result.map { it.searched_user_id }
    }
    
    suspend fun saveRecentSearch(searchedUserId: String) {
        val currentUserId = getCurrentUserId() ?: return
        
        // Delete existing if any (to update timestamp)
        supabaseClient.postgrest[SupabaseConfig.RECENT_SEARCHES_TABLE]
            .delete {
                filter {
                    eq("user_id", currentUserId)
                    eq("searched_user_id", searchedUserId)
                }
            }
        
        // Insert new
        val search = RecentSearch(
            user_id = currentUserId,
            searched_user_id = searchedUserId,
            timestamp = System.currentTimeMillis()
        )
        supabaseClient.postgrest[SupabaseConfig.RECENT_SEARCHES_TABLE]
            .insert(search)
    }
    
    suspend fun removeRecentSearch(searchedUserId: String) {
        val currentUserId = getCurrentUserId() ?: return
        supabaseClient.postgrest[SupabaseConfig.RECENT_SEARCHES_TABLE]
            .delete {
                filter {
                    eq("user_id", currentUserId)
                    eq("searched_user_id", searchedUserId)
                }
            }
    }
    
    suspend fun clearAllRecentSearches() {
        val currentUserId = getCurrentUserId() ?: return
        supabaseClient.postgrest[SupabaseConfig.RECENT_SEARCHES_TABLE]
            .delete {
                filter {
                    eq("user_id", currentUserId)
                }
            }
    }
}
