package com.junaidjamshid.i211203.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.data.dto.StoryDto
import com.junaidjamshid.i211203.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firebase Story operations.
 */
@Singleton
class FirebaseStoryDataSource @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) {
    
    private val storiesRef = firebaseDatabase.reference.child(Constants.STORIES_REF)
    
    fun getStories(): Flow<List<StoryDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                val stories = snapshot.children.mapNotNull { 
                    it.getValue(StoryDto::class.java) 
                }.filter { 
                    it.expiryTimestamp > currentTime 
                }.sortedByDescending { it.timestamp }
                trySend(stories)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        storiesRef.addValueEventListener(listener)
        
        awaitClose {
            storiesRef.removeEventListener(listener)
        }
    }
    
    suspend fun getUserStories(userId: String): List<StoryDto> {
        val currentTime = System.currentTimeMillis()
        val snapshot = storiesRef.orderByChild("userId")
            .equalTo(userId)
            .get()
            .await()
        return snapshot.children.mapNotNull { it.getValue(StoryDto::class.java) }
            .filter { it.expiryTimestamp > currentTime }
            .sortedByDescending { it.timestamp }
    }
    
    fun getUserStoriesFlow(userId: String): Flow<List<StoryDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTime = System.currentTimeMillis()
                val stories = snapshot.children
                    .mapNotNull { it.getValue(StoryDto::class.java) }
                    .filter { it.userId == userId && it.expiryTimestamp > currentTime }
                    .sortedByDescending { it.timestamp }
                trySend(stories)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        storiesRef.addValueEventListener(listener)
        
        awaitClose {
            storiesRef.removeEventListener(listener)
        }
    }
    
    suspend fun createStory(storyDto: StoryDto): StoryDto {
        val storyId = storiesRef.push().key ?: throw Exception("Failed to generate story ID")
        val story = storyDto.copy(storyId = storyId)
        storiesRef.child(storyId).setValue(story).await()
        return story
    }
    
    suspend fun deleteStory(storyId: String) {
        storiesRef.child(storyId).removeValue().await()
    }
    
    suspend fun markStoryAsViewed(storyId: String, viewerId: String) {
        storiesRef.child(storyId).child("viewers").child(viewerId).setValue(true).await()
    }
    
    suspend fun getStoryViewers(storyId: String): List<String> {
        val snapshot = storiesRef.child(storyId).child("viewers").get().await()
        return snapshot.children.mapNotNull { it.key }
    }
    
    suspend fun deleteExpiredStories() {
        val currentTime = System.currentTimeMillis()
        val snapshot = storiesRef.get().await()
        snapshot.children.forEach { storySnapshot ->
            val story = storySnapshot.getValue(StoryDto::class.java)
            if (story != null && story.expiryTimestamp <= currentTime) {
                storySnapshot.ref.removeValue().await()
            }
        }
    }
}
