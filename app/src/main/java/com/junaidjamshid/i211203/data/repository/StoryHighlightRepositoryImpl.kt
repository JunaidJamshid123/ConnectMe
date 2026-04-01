package com.junaidjamshid.i211203.data.repository

import com.junaidjamshid.i211203.data.mapper.StoryHighlightMapper.toDomain
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseStoryHighlightDataSource
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseUserDataSource
import com.junaidjamshid.i211203.domain.model.HighlightStory
import com.junaidjamshid.i211203.domain.model.StoryHighlight
import com.junaidjamshid.i211203.domain.repository.StoryHighlightRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StoryHighlightRepository using Supabase.
 */
@Singleton
class StoryHighlightRepositoryImpl @Inject constructor(
    private val highlightDataSource: SupabaseStoryHighlightDataSource,
    private val userDataSource: SupabaseUserDataSource
) : StoryHighlightRepository {
    
    override suspend fun getUserHighlights(userId: String): Resource<List<StoryHighlight>> {
        return try {
            val highlights = highlightDataSource.getUserHighlights(userId)
                .map { it.toDomain() }
            Resource.Success(highlights)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load highlights")
        }
    }
    
    override fun getUserHighlightsFlow(userId: String): Flow<Resource<List<StoryHighlight>>> {
        return highlightDataSource.getUserHighlightsFlow(userId)
            .map { highlights ->
                Resource.Success(highlights.map { it.toDomain() }) as Resource<List<StoryHighlight>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to load highlights"))
            }
    }
    
    override suspend fun getHighlight(highlightId: String): Resource<StoryHighlight> {
        return try {
            val highlight = highlightDataSource.getHighlight(highlightId)
            if (highlight != null) {
                Resource.Success(highlight.toDomain())
            } else {
                Resource.Error("Highlight not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load highlight")
        }
    }
    
    override suspend fun createHighlight(
        name: String,
        coverImageBytes: ByteArray?,
        storyImageUrls: List<String>
    ): Resource<StoryHighlight> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
                ?: return Resource.Error("User not logged in")
            
            // Upload cover image if provided
            val coverImageUrl = if (coverImageBytes != null) {
                highlightDataSource.uploadHighlightImage(currentUserId, coverImageBytes)
            } else {
                ""
            }
            
            val highlight = highlightDataSource.createHighlight(
                userId = currentUserId,
                name = name,
                coverImageUrl = coverImageUrl,
                storyImageUrls = storyImageUrls
            )
            
            Resource.Success(highlight.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create highlight")
        }
    }
    
    override suspend fun addStoriesToHighlight(
        highlightId: String,
        storyImageUrls: List<String>
    ): Resource<List<HighlightStory>> {
        return try {
            val stories = storyImageUrls.map { imageUrl ->
                highlightDataSource.addStoryToHighlight(highlightId, imageUrl).toDomain()
            }
            Resource.Success(stories)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add stories to highlight")
        }
    }
    
    override suspend fun updateHighlight(
        highlightId: String,
        name: String?,
        coverImageBytes: ByteArray?
    ): Resource<Unit> {
        return try {
            // Upload new cover image if provided
            val coverImageUrl = if (coverImageBytes != null) {
                val currentUserId = userDataSource.getCurrentUserId()
                    ?: return Resource.Error("User not logged in")
                highlightDataSource.uploadHighlightImage(currentUserId, coverImageBytes)
            } else {
                null
            }
            
            highlightDataSource.updateHighlight(highlightId, name, coverImageUrl)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update highlight")
        }
    }
    
    override suspend fun deleteHighlight(highlightId: String): Resource<Unit> {
        return try {
            highlightDataSource.deleteHighlight(highlightId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete highlight")
        }
    }
    
    override suspend fun removeStoryFromHighlight(highlightId: String, storyId: Long): Resource<Unit> {
        return try {
            highlightDataSource.removeStoryFromHighlight(highlightId, storyId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove story from highlight")
        }
    }
}
