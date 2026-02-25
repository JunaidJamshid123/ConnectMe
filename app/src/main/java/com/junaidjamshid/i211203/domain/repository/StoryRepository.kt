package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.Story
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for story-related operations.
 */
interface StoryRepository {
    
    fun getStories(userId: String): Flow<Resource<List<Story>>>
    
    fun getUserStoriesFlow(userId: String): Flow<Resource<List<Story>>>
    
    suspend fun getUserStories(userId: String): Resource<List<Story>>
    
    suspend fun createStory(imageBytes: ByteArray): Resource<Story>
    
    suspend fun createStory(
        userId: String,
        imageBase64: String
    ): Resource<Story>
    
    suspend fun deleteStory(storyId: String): Resource<Unit>
    
    suspend fun markStoryAsViewed(storyId: String): Resource<Unit>
    
    suspend fun markStoryAsViewed(storyId: String, viewerId: String): Resource<Unit>
    
    suspend fun getStoryViewers(storyId: String): Resource<List<String>>
    
    suspend fun deleteExpiredStories(): Resource<Unit>
}
