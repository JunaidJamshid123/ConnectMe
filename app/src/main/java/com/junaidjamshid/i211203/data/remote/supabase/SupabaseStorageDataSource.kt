package com.junaidjamshid.i211203.data.remote.supabase

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
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
    
    companion object {
        private const val TAG = "SupabaseStorage"
        private const val VIDEOS_BUCKET = "videos"
        private const val THUMBNAILS_BUCKET = "video-thumbnails"
    }
    
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
     * Upload a voice message
     * NOTE: Using message-images bucket temporarily until voice-messages bucket is set up
     */
    suspend fun uploadVoiceMessage(conversationId: String, audioBytes: ByteArray): String {
        val fileName = "voice_${conversationId}_${UUID.randomUUID()}.3gp"
        // Use message-images bucket (same as images) - change to VOICE_MESSAGES_BUCKET when bucket is ready
        return uploadFile(SupabaseConfig.MESSAGE_IMAGES_BUCKET, fileName, audioBytes)
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
    
    // ==================== VIDEO UPLOAD METHODS ====================
    
    /**
     * Upload a video file to Supabase Storage.
     * For large files, this streams the data to avoid OutOfMemory errors.
     */
    suspend fun uploadVideo(
        videoFile: File,
        userId: String,
        postId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val fileSizeMb = videoFile.length() / (1024.0 * 1024.0)
            Log.d(TAG, "Starting video upload: ${videoFile.name} (${String.format("%.2f", fileSizeMb)} MB)")
            
            val videoBytes = videoFile.readBytes()
            Log.d(TAG, "Video bytes read into memory: ${videoBytes.size} bytes")
            
            val storagePath = "posts/$userId/$postId/video.mp4"
            Log.d(TAG, "Uploading to bucket '$VIDEOS_BUCKET' at path: $storagePath")
            
            val bucket = supabaseClient.storage.from(VIDEOS_BUCKET)
            
            Log.d(TAG, "Calling Supabase upload...")
            bucket.upload(storagePath, videoBytes) {
                upsert = true
            }
            Log.d(TAG, "Supabase upload completed!")
            
            val publicUrl = bucket.publicUrl(storagePath)
            Log.d(TAG, "Video uploaded successfully: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload video: ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Upload a video from URI to Supabase Storage.
     */
    suspend fun uploadVideoFromUri(
        context: Context,
        videoUri: Uri,
        userId: String,
        postId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(videoUri)
                ?: throw Exception("Failed to open video URI")
            
            val videoBytes = inputStream.use { it.readBytes() }
            val storagePath = "posts/$userId/$postId/video.mp4"
            
            val bucket = supabaseClient.storage.from(VIDEOS_BUCKET)
            bucket.upload(storagePath, videoBytes) {
                upsert = true
            }
            
            val publicUrl = bucket.publicUrl(storagePath)
            Log.d(TAG, "Video uploaded successfully: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload video from URI: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Upload a video thumbnail to Supabase Storage.
     */
    suspend fun uploadVideoThumbnail(
        thumbnailBitmap: Bitmap,
        userId: String,
        postId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val outputStream = ByteArrayOutputStream()
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val thumbnailBytes = outputStream.toByteArray()
            
            val storagePath = "posts/$userId/$postId/thumbnail.jpg"
            
            val bucket = supabaseClient.storage.from(THUMBNAILS_BUCKET)
            bucket.upload(storagePath, thumbnailBytes) {
                upsert = true
            }
            
            val publicUrl = bucket.publicUrl(storagePath)
            Log.d(TAG, "Thumbnail uploaded successfully: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload thumbnail: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Extract a thumbnail from a video file.
     */
    fun extractVideoThumbnail(videoFile: File, timeMs: Long = 500L): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(
                timeMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            retriever.release()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail: ${e.message}", e)
            null
        }
    }
    
    /**
     * Extract a thumbnail from a video URI.
     */
    fun extractVideoThumbnailFromUri(context: Context, videoUri: Uri, timeMs: Long = 500L): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val bitmap = retriever.getFrameAtTime(
                timeMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            retriever.release()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail from URI: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get video metadata from a file.
     */
    fun getVideoMetadata(videoFile: File): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            extractVideoMetadata(retriever)
        } catch (e: Exception) {
            retriever.release()
            Log.e(TAG, "Failed to get video metadata: ${e.message}", e)
            VideoMetadata(0, 0, 0)
        }
    }
    
    /**
     * Get video metadata from a URI.
     */
    fun getVideoMetadataFromUri(context: Context, videoUri: Uri): VideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            extractVideoMetadata(retriever)
        } catch (e: Exception) {
            retriever.release()
            Log.e(TAG, "Failed to get video metadata from URI: ${e.message}", e)
            VideoMetadata(0, 0, 0)
        }
    }
    
    private fun extractVideoMetadata(retriever: MediaMetadataRetriever): VideoMetadata {
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        
        val finalWidth = if (rotation == 90 || rotation == 270) height else width
        val finalHeight = if (rotation == 90 || rotation == 270) width else height
        
        retriever.release()
        
        return VideoMetadata(
            durationMs = duration.toInt(),
            width = finalWidth,
            height = finalHeight
        )
    }
    
    /**
     * Delete a video from storage.
     */
    suspend fun deleteVideo(userId: String, postId: String) = withContext(Dispatchers.IO) {
        try {
            val bucket = supabaseClient.storage.from(VIDEOS_BUCKET)
            bucket.delete("posts/$userId/$postId/video.mp4")
            Log.d(TAG, "Video deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete video: ${e.message}", e)
        }
    }
    
    /**
     * Delete a video thumbnail from storage.
     */
    suspend fun deleteVideoThumbnail(userId: String, postId: String) = withContext(Dispatchers.IO) {
        try {
            val bucket = supabaseClient.storage.from(THUMBNAILS_BUCKET)
            bucket.delete("posts/$userId/$postId/thumbnail.jpg")
            Log.d(TAG, "Thumbnail deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete thumbnail: ${e.message}", e)
        }
    }
}

/**
 * Video metadata containing duration and dimensions.
 */
data class VideoMetadata(
    val durationMs: Int,
    val width: Int,
    val height: Int
) {
    val aspectRatio: Float
        get() = if (width > 0) height.toFloat() / width else 1f
    
    val isVertical: Boolean
        get() = height > width
    
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }
}
