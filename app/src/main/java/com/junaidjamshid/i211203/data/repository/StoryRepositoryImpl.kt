package com.junaidjamshid.i211203.data.repository

import android.util.Base64
import com.junaidjamshid.i211203.data.dto.StoryDto
import com.junaidjamshid.i211203.data.mapper.StoryMapper.toDomain
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseStoryDataSource
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseUserDataSource
import com.junaidjamshid.i211203.domain.model.Story
import com.junaidjamshid.i211203.domain.repository.StoryRepository
import com.junaidjamshid.i211203.util.Constants
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StoryRepository that uses Supabase.
 */
@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val storyDataSource: SupabaseStoryDataSource,
    private val userDataSource: SupabaseUserDataSource
) : StoryRepository {
    
    override fun getStories(userId: String): Flow<Resource<List<Story>>> {
        return storyDataSource.getStories()
            .map { stories ->
                Resource.Success(stories.map { it.toDomain(userId) }) as Resource<List<Story>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to load stories"))
            }
    }
    
    override fun getUserStoriesFlow(userId: String): Flow<Resource<List<Story>>> {
        return storyDataSource.getUserStoriesFlow(userId)
            .map { stories ->
                Resource.Success(stories.map { it.toDomain(userId) }) as Resource<List<Story>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to get user stories"))
            }
    }
    
    override suspend fun getUserStories(userId: String): Resource<List<Story>> {
        return try {
            val stories = storyDataSource.getUserStories(userId).map { it.toDomain(userId) }
            Resource.Success(stories)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user stories")
        }
    }
    
    override suspend fun createStory(imageBytes: ByteArray): Resource<Story> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                createStory(currentUserId, imageBase64)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create story")
        }
    }
    
    override suspend fun createStory(userId: String, imageBase64: String): Resource<Story> {
        return try {
            val user = userDataSource.getUserById(userId)
            val currentTime = System.currentTimeMillis()
            val storyDto = StoryDto(
                storyId = "",
                userId = userId,
                username = user?.username ?: "",
                userProfileImage = user?.profilePicture ?: "",
                storyImageUrl = imageBase64,
                timestamp = currentTime,
                expiryTimestamp = currentTime + Constants.STORY_EXPIRATION_TIME
            )
            val createdStory = storyDataSource.createStory(storyDto)
            Resource.Success(createdStory.toDomain(userId))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create story")
        }
    }
    
    override suspend fun deleteStory(storyId: String): Resource<Unit> {
        return try {
            storyDataSource.deleteStory(storyId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete story")
        }
    }
    
    override suspend fun markStoryAsViewed(storyId: String): Resource<Unit> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                storyDataSource.markStoryAsViewed(storyId, currentUserId)
                Resource.Success(Unit)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark story as viewed")
        }
    }
    
    override suspend fun markStoryAsViewed(storyId: String, viewerId: String): Resource<Unit> {
        return try {
            storyDataSource.markStoryAsViewed(storyId, viewerId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark story as viewed")
        }
    }
    
    override suspend fun getStoryViewers(storyId: String): Resource<List<String>> {
        return try {
            val viewers = storyDataSource.getStoryViewersList(storyId)
            Resource.Success(viewers)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get story viewers")
        }
    }
    
    override suspend fun deleteExpiredStories(): Resource<Unit> {
        return try {
            storyDataSource.deleteExpiredStories()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete expired stories")
        }
    }
}
