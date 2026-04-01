package com.junaidjamshid.i211203.data.remote.supabase

import android.util.Log
import com.junaidjamshid.i211203.data.dto.HighlightStoryDto
import com.junaidjamshid.i211203.data.dto.StoryHighlightDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase representation of Story Highlight.
 */
@Serializable
data class SupabaseStoryHighlight(
    val highlight_id: String = "",
    val user_id: String = "",
    val name: String = "",
    val cover_image_url: String = "",
    val created_at: Long = 0,
    val updated_at: Long = 0,
    val position: Int = 0
)

/**
 * Supabase representation of a story within a highlight.
 */
@Serializable
data class SupabaseHighlightStory(
    val id: Long = 0,
    val highlight_id: String = "",
    val story_image_url: String = "",
    val timestamp: Long = 0,
    val position: Int = 0
)

/**
 * Insert model for highlight_stories (excludes 'id' for auto-generation).
 */
@Serializable
data class SupabaseHighlightStoryInsert(
    val highlight_id: String,
    val story_image_url: String,
    val timestamp: Long = System.currentTimeMillis(),
    val position: Int = 0
)

/**
 * Insert model for story_highlights (excludes 'id' for auto-generation).
 */
@Serializable
data class SupabaseStoryHighlightInsert(
    val highlight_id: String,
    val user_id: String,
    val name: String,
    val cover_image_url: String = "",
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val position: Int = 0
)

/**
 * Convert SupabaseStoryHighlight to DTO
 */
fun SupabaseStoryHighlight.toDto(stories: List<HighlightStoryDto> = emptyList()): StoryHighlightDto =
    StoryHighlightDto(
        highlightId = highlight_id,
        userId = user_id,
        name = name,
        coverImageUrl = cover_image_url,
        stories = stories,
        createdAt = created_at,
        updatedAt = updated_at,
        position = position
    )

/**
 * Convert SupabaseHighlightStory to DTO
 */
fun SupabaseHighlightStory.toDto(): HighlightStoryDto = HighlightStoryDto(
    id = id,
    highlightId = highlight_id,
    storyImageUrl = story_image_url,
    timestamp = timestamp,
    position = position
)

/**
 * Data source for Supabase Story Highlight operations.
 */
@Singleton
class SupabaseStoryHighlightDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    companion object {
        private const val TAG = "HighlightDataSource"
    }
    
    /**
     * Get all highlights for a user
     */
    suspend fun getUserHighlights(userId: String): List<StoryHighlightDto> {
        return try {
            val highlights = supabaseClient.postgrest[SupabaseConfig.STORY_HIGHLIGHTS_TABLE]
                .select {
                    filter { eq("user_id", userId) }
                    order("position", Order.ASCENDING)
                }
                .decodeList<SupabaseStoryHighlight>()
            
            Log.d(TAG, "getUserHighlights: found ${highlights.size} highlights for user $userId")
            
            // Fetch stories for each highlight
            highlights.map { highlight ->
                val stories = getHighlightStories(highlight.highlight_id)
                highlight.toDto(stories)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserHighlights failed: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get highlights as a Flow for real-time updates
     */
    fun getUserHighlightsFlow(userId: String): Flow<List<StoryHighlightDto>> = flow {
        emit(getUserHighlights(userId))
        
        val channel = supabaseClient.realtime.channel("user-highlights-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.STORY_HIGHLIGHTS_TABLE
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getUserHighlights(userId))
        }
    }
    
    /**
     * Get a single highlight by ID
     */
    suspend fun getHighlight(highlightId: String): StoryHighlightDto? {
        return try {
            val highlight = supabaseClient.postgrest[SupabaseConfig.STORY_HIGHLIGHTS_TABLE]
                .select {
                    filter { eq("highlight_id", highlightId) }
                }
                .decodeSingleOrNull<SupabaseStoryHighlight>()
            
            highlight?.let {
                val stories = getHighlightStories(highlightId)
                it.toDto(stories)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getHighlight failed: ${e.message}")
            null
        }
    }
    
    /**
     * Get all stories within a highlight
     */
    suspend fun getHighlightStories(highlightId: String): List<HighlightStoryDto> {
        return try {
            val stories = supabaseClient.postgrest[SupabaseConfig.HIGHLIGHT_STORIES_TABLE]
                .select {
                    filter { eq("highlight_id", highlightId) }
                    order("position", Order.ASCENDING)
                }
                .decodeList<SupabaseHighlightStory>()
            
            stories.map { it.toDto() }
        } catch (e: Exception) {
            Log.e(TAG, "getHighlightStories failed: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Create a new highlight
     */
    suspend fun createHighlight(
        userId: String,
        name: String,
        coverImageUrl: String = "",
        storyImageUrls: List<String> = emptyList()
    ): StoryHighlightDto {
        val highlightId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        
        // Get current max position for user
        val maxPosition = try {
            val existing = supabaseClient.postgrest[SupabaseConfig.STORY_HIGHLIGHTS_TABLE]
                .select {
                    filter { eq("user_id", userId) }
                    order("position", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<SupabaseStoryHighlight>()
            existing.firstOrNull()?.position ?: -1
        } catch (e: Exception) {
            -1
        }
        
        val highlightInsert = SupabaseStoryHighlightInsert(
            highlight_id = highlightId,
            user_id = userId,
            name = name,
            cover_image_url = coverImageUrl.ifEmpty { storyImageUrls.firstOrNull() ?: "" },
            created_at = currentTime,
            updated_at = currentTime,
            position = maxPosition + 1
        )
        
        supabaseClient.postgrest[SupabaseConfig.STORY_HIGHLIGHTS_TABLE]
            .insert(highlightInsert)
        
        // Add stories to the highlight
        val stories = storyImageUrls.mapIndexed { index, imageUrl ->
            addStoryToHighlight(highlightId, imageUrl, index)
        }
        
        Log.d(TAG, "createHighlight: created highlight $highlightId with ${stories.size} stories")
        
        return StoryHighlightDto(
            highlightId = highlightId,
            userId = userId,
            name = name,
            coverImageUrl = highlightInsert.cover_image_url,
            stories = stories,
            createdAt = currentTime,
            updatedAt = currentTime,
            position = highlightInsert.position
        )
    }
    
    /**
     * Add a story to an existing highlight
     */
    suspend fun addStoryToHighlight(
        highlightId: String,
        storyImageUrl: String,
        position: Int = -1
    ): HighlightStoryDto {
        val actualPosition = if (position < 0) {
            // Get max position
            val existing = getHighlightStories(highlightId)
            existing.maxOfOrNull { it.position }?.plus(1) ?: 0
        } else {
            position
        }
        
        val storyInsert = SupabaseHighlightStoryInsert(
            highlight_id = highlightId,
            story_image_url = storyImageUrl,
            timestamp = System.currentTimeMillis(),
            position = actualPosition
        )
        
        supabaseClient.postgrest[SupabaseConfig.HIGHLIGHT_STORIES_TABLE]
            .insert(storyInsert)
        
        // Update highlight's updated_at timestamp
        updateHighlightTimestamp(highlightId)
        
        return HighlightStoryDto(
            id = 0, // Will be auto-generated
            highlightId = highlightId,
            storyImageUrl = storyImageUrl,
            timestamp = storyInsert.timestamp,
            position = actualPosition
        )
    }
    
    /**
     * Update a highlight's name or cover image
     */
    suspend fun updateHighlight(
        highlightId: String,
        name: String? = null,
        coverImageUrl: String? = null
    ) {
        val updates = mutableMapOf<String, Any>("updated_at" to System.currentTimeMillis())
        name?.let { updates["name"] = it }
        coverImageUrl?.let { updates["cover_image_url"] = it }
        
        supabaseClient.postgrest[SupabaseConfig.STORY_HIGHLIGHTS_TABLE]
            .update(updates) {
                filter { eq("highlight_id", highlightId) }
            }
    }
    
    /**
     * Delete a highlight and all its stories
     */
    suspend fun deleteHighlight(highlightId: String) {
        // Stories are deleted automatically due to CASCADE
        supabaseClient.postgrest[SupabaseConfig.STORY_HIGHLIGHTS_TABLE]
            .delete {
                filter { eq("highlight_id", highlightId) }
            }
        
        Log.d(TAG, "deleteHighlight: deleted highlight $highlightId")
    }
    
    /**
     * Remove a story from a highlight
     */
    suspend fun removeStoryFromHighlight(highlightId: String, storyId: Long) {
        supabaseClient.postgrest[SupabaseConfig.HIGHLIGHT_STORIES_TABLE]
            .delete {
                filter {
                    eq("id", storyId)
                    eq("highlight_id", highlightId)
                }
            }
        
        // Update highlight's updated_at timestamp
        updateHighlightTimestamp(highlightId)
    }
    
    /**
     * Update highlight's updated_at timestamp
     */
    private suspend fun updateHighlightTimestamp(highlightId: String) {
        supabaseClient.postgrest[SupabaseConfig.STORY_HIGHLIGHTS_TABLE]
            .update(mapOf("updated_at" to System.currentTimeMillis())) {
                filter { eq("highlight_id", highlightId) }
            }
    }
    
    /**
     * Upload a highlight cover image
     */
    suspend fun uploadHighlightImage(userId: String, imageBytes: ByteArray): String {
        val fileName = "highlight_${userId}_${UUID.randomUUID()}.jpg"
        val bucket = supabaseClient.storage.from(SupabaseConfig.HIGHLIGHT_IMAGES_BUCKET)
        bucket.upload(fileName, imageBytes) { upsert = true }
        return bucket.publicUrl(fileName)
    }
}
