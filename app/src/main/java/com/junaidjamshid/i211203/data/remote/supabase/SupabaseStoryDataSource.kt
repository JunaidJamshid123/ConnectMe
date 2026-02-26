package com.junaidjamshid.i211203.data.remote.supabase

import com.junaidjamshid.i211203.data.dto.StoryDto
import io.github.jan.supabase.SupabaseClient
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
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase representation of Story.
 */
@Serializable
data class SupabaseStory(
    val story_id: String = "",
    val user_id: String = "",
    val username: String = "",
    val user_profile_image: String = "",
    val story_image_url: String = "",
    val timestamp: Long = 0,
    val expiry_timestamp: Long = 0
)

/**
 * Supabase representation of Story Viewer.
 */
@Serializable
data class StoryViewer(
    val id: Long? = null,
    val story_id: String,
    val viewer_id: String,
    val viewed_at: Long = System.currentTimeMillis()
)

fun SupabaseStory.toDto(viewers: Map<String, Boolean> = emptyMap()): StoryDto = StoryDto(
    storyId = story_id,
    userId = user_id,
    username = username,
    userProfileImage = user_profile_image,
    storyImageUrl = story_image_url,
    timestamp = timestamp,
    expiryTimestamp = expiry_timestamp,
    viewers = viewers.toMutableMap()
)

/**
 * Data source for Supabase Story operations.
 */
@Singleton
class SupabaseStoryDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    private fun getStoryViewers(storyId: String): Map<String, Boolean> {
        // Synchronous version for internal use
        return emptyMap()
    }
    
    suspend fun getStoryViewersList(storyId: String): List<String> {
        val viewers = supabaseClient.postgrest[SupabaseConfig.STORY_VIEWERS_TABLE]
            .select(columns = Columns.list("viewer_id")) {
                filter {
                    eq("story_id", storyId)
                }
            }
            .decodeList<Map<String, String>>()
        return viewers.mapNotNull { it["viewer_id"] }
    }
    
    private suspend fun getActiveStories(): List<StoryDto> {
        val currentTime = System.currentTimeMillis()
        val stories = supabaseClient.postgrest[SupabaseConfig.STORIES_TABLE]
            .select {
                filter {
                    gt("expiry_timestamp", currentTime)
                }
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<SupabaseStory>()
        
        return stories.map { story ->
            val viewers = getStoryViewers(story.story_id)
            story.toDto(viewers)
        }
    }
    
    fun getStories(): Flow<List<StoryDto>> = flow {
        emit(getActiveStories())
        
        val channel = supabaseClient.realtime.channel("stories")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.STORIES_TABLE
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getActiveStories())
        }
    }
    
    suspend fun getUserStories(userId: String): List<StoryDto> {
        val currentTime = System.currentTimeMillis()
        val stories = supabaseClient.postgrest[SupabaseConfig.STORIES_TABLE]
            .select {
                filter {
                    eq("user_id", userId)
                    gt("expiry_timestamp", currentTime)
                }
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<SupabaseStory>()
        
        return stories.map { story ->
            val viewers = getStoryViewers(story.story_id)
            story.toDto(viewers)
        }
    }
    
    fun getUserStoriesFlow(userId: String): Flow<List<StoryDto>> = flow {
        emit(getUserStories(userId))
        
        val channel = supabaseClient.realtime.channel("user-stories-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.STORIES_TABLE
            filter("user_id", FilterOperator.EQ, userId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getUserStories(userId))
        }
    }
    
    suspend fun createStory(storyDto: StoryDto): StoryDto {
        val storyId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        val expiryTime = currentTime + (24 * 60 * 60 * 1000) // 24 hours
        
        val supabaseStory = SupabaseStory(
            story_id = storyId,
            user_id = storyDto.userId,
            username = storyDto.username,
            user_profile_image = storyDto.userProfileImage,
            story_image_url = storyDto.storyImageUrl,
            timestamp = currentTime,
            expiry_timestamp = expiryTime
        )
        
        supabaseClient.postgrest[SupabaseConfig.STORIES_TABLE]
            .insert(supabaseStory)
        
        return storyDto.copy(
            storyId = storyId,
            timestamp = currentTime,
            expiryTimestamp = expiryTime
        )
    }
    
    suspend fun deleteStory(storyId: String) {
        // Delete viewers first
        supabaseClient.postgrest[SupabaseConfig.STORY_VIEWERS_TABLE]
            .delete {
                filter {
                    eq("story_id", storyId)
                }
            }
        
        // Delete story
        supabaseClient.postgrest[SupabaseConfig.STORIES_TABLE]
            .delete {
                filter {
                    eq("story_id", storyId)
                }
            }
    }
    
    suspend fun markStoryAsViewed(storyId: String, viewerId: String) {
        // Check if already viewed
        val existing = supabaseClient.postgrest[SupabaseConfig.STORY_VIEWERS_TABLE]
            .select {
                filter {
                    eq("story_id", storyId)
                    eq("viewer_id", viewerId)
                }
            }
            .decodeList<StoryViewer>()
        
        if (existing.isEmpty()) {
            val viewer = StoryViewer(
                story_id = storyId,
                viewer_id = viewerId
            )
            supabaseClient.postgrest[SupabaseConfig.STORY_VIEWERS_TABLE]
                .insert(viewer)
        }
    }
    
    suspend fun deleteExpiredStories() {
        val currentTime = System.currentTimeMillis()
        
        // Get expired story IDs
        val expiredStories = supabaseClient.postgrest[SupabaseConfig.STORIES_TABLE]
            .select(columns = Columns.list("story_id")) {
                filter {
                    lte("expiry_timestamp", currentTime)
                }
            }
            .decodeList<Map<String, String>>()
        
        val expiredIds = expiredStories.mapNotNull { it["story_id"] }
        
        // Delete viewers for expired stories
        expiredIds.forEach { storyId ->
            supabaseClient.postgrest[SupabaseConfig.STORY_VIEWERS_TABLE]
                .delete {
                    filter {
                        eq("story_id", storyId)
                    }
                }
        }
        
        // Delete expired stories
        supabaseClient.postgrest[SupabaseConfig.STORIES_TABLE]
            .delete {
                filter {
                    lte("expiry_timestamp", currentTime)
                }
            }
    }
}
