package com.junaidjamshid.i211203.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility class for compressing videos before upload.
 * Uses LightCompressor library for efficient compression.
 */
object VideoCompressorUtil {
    
    private const val TAG = "VideoCompressorUtil"
    
    /**
     * Compression result containing the compressed video path and metadata.
     */
    data class CompressionResult(
        val compressedPath: String,
        val originalSize: Long,
        val compressedSize: Long,
        val compressionRatio: Float
    )
    
    /**
     * Compress a video file for optimal upload size.
     * 
     * @param context Application context
     * @param videoUri URI of the video to compress
     * @param quality Target quality (LOW, MEDIUM, HIGH, VERY_HIGH)
     * @param maxDuration Maximum duration in milliseconds (0 = no limit)
     * @param onProgress Callback for compression progress (0-100)
     * @return CompressionResult with compressed video path
     */
    suspend fun compressVideo(
        context: Context,
        videoUri: Uri,
        quality: VideoQuality = VideoQuality.MEDIUM,
        maxDuration: Long = 60_000L, // 60 seconds default for feed posts
        onProgress: ((Float) -> Unit)? = null
    ): CompressionResult = withContext(Dispatchers.IO) {
        
        suspendCancellableCoroutine { continuation ->
            
            val outputDir = File(context.cacheDir, "compressed_videos").apply {
                if (!exists()) mkdirs()
            }
            
            val outputFileName = "compressed_${System.currentTimeMillis()}.mp4"
            
            VideoCompressor.start(
                context = context,
                uris = listOf(videoUri),
                isStreamable = true,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    subFolderName = "ConnectMe"
                ),
                configureWith = Configuration(
                    quality = quality,
                    isMinBitrateCheckEnabled = false,
                    videoBitrateInMbps = 4,  // Target 4 Mbps for good quality
                    disableAudio = false,
                    keepOriginalResolution = false,
                    videoWidth = 720.0,      // 720p max width
                    videoHeight = 1280.0,     // 720p max height (portrait)
                    videoNames = listOf(outputFileName)
                ),

                listener = object : CompressionListener {
                    override fun onStart(index: Int) {
                        Log.d(TAG, "Compression started for video $index")
                    }
                    
                    override fun onProgress(index: Int, percent: Float) {
                        onProgress?.invoke(percent)
                        Log.d(TAG, "Compression progress: $percent%")
                    }
                    
                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        Log.d(TAG, "Compression success! Size: $size, Path: $path")
                        if (path != null) {
                            // Handle _temp suffix - LightCompressor returns temp path but actual file doesn't have it
                            var finalPath = path
                            val tempFile = File(path)
                            val actualFile = File(path.removeSuffix("_temp"))
                            
                            Log.d(TAG, "Checking paths:")
                            Log.d(TAG, "  Temp path: $path, exists: ${tempFile.exists()}")
                            Log.d(TAG, "  Actual path: ${actualFile.absolutePath}, exists: ${actualFile.exists()}")
                            
                            // Try different possible paths
                            finalPath = when {
                                actualFile.exists() -> actualFile.absolutePath
                                tempFile.exists() -> path
                                else -> {
                                    // Try the path without _temp suffix
                                    val noTempPath = path.replace("_temp", "")
                                    val noTempFile = File(noTempPath)
                                    if (noTempFile.exists()) noTempPath else path
                                }
                            }
                            
                            // Verify file exists
                            val outputFile = File(finalPath)
                            if (!outputFile.exists()) {
                                // Last resort: check if file exists at the Movies/ConnectMe folder
                                val moviesDir = File("/storage/emulated/0/Movies/ConnectMe")
                                val files = moviesDir.listFiles()?.filter { it.name.startsWith("compressed_") }
                                Log.d(TAG, "Files in ConnectMe folder: ${files?.map { it.name }}")
                                
                                // Get most recent file
                                val mostRecent = files?.maxByOrNull { it.lastModified() }
                                if (mostRecent != null && mostRecent.exists()) {
                                    finalPath = mostRecent.absolutePath
                                    Log.d(TAG, "Using most recent file: $finalPath")
                                } else {
                                    Log.e(TAG, "Output file does not exist: $finalPath")
                                    continuation.resumeWithException(
                                        Exception("Compressed file not found at: $finalPath")
                                    )
                                    return
                                }
                            }
                            
                            val originalFile = File(videoUri.path ?: "")
                            val originalSize = originalFile.length().takeIf { it > 0 } ?: size * 2
                            val compressedFile = File(finalPath)
                            
                            Log.d(TAG, "Compression complete. Final path: $finalPath, size: ${compressedFile.length()}")
                            
                            continuation.resume(
                                CompressionResult(
                                    compressedPath = finalPath,
                                    originalSize = originalSize,
                                    compressedSize = compressedFile.length(),
                                    compressionRatio = (originalSize - compressedFile.length()).toFloat() / originalSize
                                )
                            )
                        } else {
                            continuation.resumeWithException(
                                Exception("Compression succeeded but no output path returned")
                            )
                        }
                    }
                    
                    override fun onFailure(index: Int, failureMessage: String) {
                        Log.e(TAG, "Compression failed: $failureMessage")
                        continuation.resumeWithException(Exception(failureMessage))
                    }
                    
                    override fun onCancelled(index: Int) {
                        Log.d(TAG, "Compression cancelled")
                        continuation.resumeWithException(Exception("Compression cancelled"))
                    }
                }
            )
            
            continuation.invokeOnCancellation {
                VideoCompressor.cancel()
            }
        }
    }
    
    /**
     * Get optimal quality based on original video size and network conditions.
     */
    fun getOptimalQuality(originalSizeBytes: Long, isOnWifi: Boolean): VideoQuality {
        val sizeMb = originalSizeBytes / (1024 * 1024)
        
        return when {
            sizeMb > 100 -> VideoQuality.LOW
            sizeMb > 50 -> VideoQuality.MEDIUM
            sizeMb > 20 && !isOnWifi -> VideoQuality.MEDIUM
            else -> VideoQuality.HIGH
        }
    }
    
    /**
     * Clean up temporary compressed video files.
     */
    fun cleanupTempFiles(context: Context) {
        val tempDir = File(context.cacheDir, "compressed_videos")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                // Delete files older than 24 hours
                if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000) {
                    file.delete()
                }
            }
        }
    }
}
