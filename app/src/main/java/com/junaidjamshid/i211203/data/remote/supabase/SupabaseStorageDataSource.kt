package com.junaidjamshid.i211203.data.remote.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Supabase Storage operations.
 */
@Singleton
class SupabaseStorageDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    /**
     * Upload a profile image
     */
    suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): String {
        val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
        return uploadFile(SupabaseConfig.PROFILE_IMAGES_BUCKET, fileName, imageBytes)
    }
    
    /**
     * Upload a cover photo
     */
    suspend fun uploadCoverPhoto(userId: String, imageBytes: ByteArray): String {
        val fileName = "cover_${userId}_${System.currentTimeMillis()}.jpg"
        return uploadFile(SupabaseConfig.PROFILE_IMAGES_BUCKET, fileName, imageBytes)
    }
    
    /**
     * Upload a post image
     */
    suspend fun uploadPostImage(userId: String, imageBytes: ByteArray): String {
        val fileName = "post_${userId}_${UUID.randomUUID()}.jpg"
        return uploadFile(SupabaseConfig.POST_IMAGES_BUCKET, fileName, imageBytes)
    }
    
    /**
     * Upload a story image
     */
    suspend fun uploadStoryImage(userId: String, imageBytes: ByteArray): String {
        val fileName = "story_${userId}_${UUID.randomUUID()}.jpg"
        return uploadFile(SupabaseConfig.STORY_IMAGES_BUCKET, fileName, imageBytes)
    }
    
    /**
     * Upload a message image
     */
    suspend fun uploadMessageImage(conversationId: String, imageBytes: ByteArray): String {
        val fileName = "message_${conversationId}_${UUID.randomUUID()}.jpg"
        return uploadFile(SupabaseConfig.MESSAGE_IMAGES_BUCKET, fileName, imageBytes)
    }
    
    /**
     * Generic file upload
     */
    private suspend fun uploadFile(bucket: String, fileName: String, fileBytes: ByteArray): String {
        val storage = supabaseClient.storage
        storage.from(bucket).upload(fileName, fileBytes) { upsert = true }
        return storage.from(bucket).publicUrl(fileName)
    }
    
    /**
     * Delete a file from storage
     */
    suspend fun deleteFile(bucket: String, fileName: String) {
        supabaseClient.storage.from(bucket).delete(fileName)
    }
    
    /**
     * Get public URL for a file
     */
    fun getPublicUrl(bucket: String, fileName: String): String {
        return supabaseClient.storage.from(bucket).publicUrl(fileName)
    }
}
