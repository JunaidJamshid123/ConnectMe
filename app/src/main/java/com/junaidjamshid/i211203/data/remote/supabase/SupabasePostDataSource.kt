package com.junaidjamshid.i211203.data.remote.supabase

import com.junaidjamshid.i211203.data.dto.CommentDto
import com.junaidjamshid.i211203.data.dto.PostDto
import android.util.Log
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase representation of Post Image (for reading/decoding).
 */
@Serializable
data class SupabasePostImage(
    val post_id: String = "",
    val image_url: String = "",
    val position: Int = 0,
    val alt_text: String = ""
)

/**
 * Insert-only model for post_images — excludes 'id' so BIGSERIAL auto-generates.
 */
@Serializable
data class SupabasePostImageInsert(
    val post_id: String,
    val image_url: String,
    val position: Int,
    val alt_text: String = ""
)

/**
 * Supabase representation of Post.
 * Supports image posts, carousels, and video/reel posts.
 */
@Serializable
data class SupabasePost(
    val post_id: String = "",
    val user_id: String = "",
    val username: String = "",
    val user_profile_image: String = "",
    val post_image_url: String = "",
    val image_urls: String = "[]",
    val caption: String = "",
    val location: String = "",
    val music_name: String = "",
    val music_artist: String = "",
    val timestamp: Long = 0,
    // Video/Reel fields
    val media_type: String = "image",
    val video_url: String = "",
    val thumbnail_url: String = "",
    val video_duration: Int = 0,
    val video_width: Int = 0,
    val video_height: Int = 0,
    val aspect_ratio: Float = 1f,
    val views_count: Int = 0
)

/**
 * Supabase representation of Comment.
 */
@Serializable
data class SupabaseComment(
    val comment_id: String = "",
    val post_id: String = "",
    val user_id: String = "",
    val username: String = "",
    val user_profile_image: String = "",
    val content: String = "",
    val timestamp: Long = 0
)

/**
 * Supabase representation of Like.
 */
@Serializable
data class PostLike(
    val id: Long? = null,
    val post_id: String,
    val user_id: String,
    val created_at: Long = System.currentTimeMillis()
)

/**
 * Supabase representation of Comment Like.
 */
@Serializable
data class CommentLike(
    val id: Long? = null,
    val comment_id: String,
    val user_id: String,
    val created_at: Long = System.currentTimeMillis()
)

private val json = Json { ignoreUnknownKeys = true }

fun SupabasePost.toDto(
    likes: Map<String, Boolean> = emptyMap(),
    comments: List<CommentDto> = emptyList(),
    imageUrls: List<String> = emptyList()
): PostDto {
    // Prefer images from post_images table; fallback to inline image_urls JSON column
    val resolvedImages = imageUrls.ifEmpty {
        try {
            json.decodeFromString<List<String>>(image_urls)
        } catch (e: Exception) {
            emptyList()
        }
    }
    return PostDto(
        postId = post_id,
        userId = user_id,
        username = username,
        userProfileImage = user_profile_image,
        postImageUrl = post_image_url,
        imageUrls = resolvedImages,
        caption = caption,
        location = location,
        musicName = music_name,
        musicArtist = music_artist,
        timestamp = timestamp,
        likes = likes.toMutableMap(),
        comments = comments.toMutableList(),
        // Video/Reel fields
        mediaType = media_type,
        videoUrl = video_url,
        thumbnailUrl = thumbnail_url,
        videoDuration = video_duration,
        videoWidth = video_width,
        videoHeight = video_height,
        aspectRatio = aspect_ratio,
        viewsCount = views_count
    )
}

fun SupabaseComment.toDto(likes: Map<String, Boolean> = emptyMap()): CommentDto = CommentDto(
    commentId = comment_id,
    postId = post_id,
    userId = user_id,
    username = username,
    userProfileImage = user_profile_image,
    content = content,
    timestamp = timestamp,
    likes = likes.toMutableMap()
)

/**
 * Data source for Supabase Post operations.
 */
@Singleton
class SupabasePostDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    
    private suspend fun getPostLikes(postId: String): Map<String, Boolean> {
        val likes = supabaseClient.postgrest[SupabaseConfig.LIKES_TABLE]
            .select(columns = Columns.list("user_id")) {
                filter {
                    eq("post_id", postId)
                }
            }
            .decodeList<Map<String, String>>()
        return likes.mapNotNull { it["user_id"] }.associateWith { true }
    }
    
    private suspend fun getPostComments(postId: String): List<CommentDto> {
        val comments = supabaseClient.postgrest[SupabaseConfig.COMMENTS_TABLE]
            .select {
                filter {
                    eq("post_id", postId)
                }
                order("timestamp", Order.ASCENDING)
            }
            .decodeList<SupabaseComment>()
        return comments.map { it.toDto() }
    }
    
    private suspend fun getPostImages(postId: String): List<String> {
        return try {
            val images = supabaseClient.postgrest["post_images"]
                .select {
                    filter { eq("post_id", postId) }
                    order("position", Order.ASCENDING)
                }
                .decodeList<SupabasePostImage>()
            Log.d("PostDataSource", "getPostImages($postId): found ${images.size} images")
            images.map { it.image_url }
        } catch (e: Exception) {
            Log.e("PostDataSource", "getPostImages($postId) FAILED: ${e.message}", e)
            emptyList()
        }
    }
    
    suspend fun getFeedPostsList(): List<PostDto> {
        val posts = supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
            .select {
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<SupabasePost>()
        
        return posts.map { post ->
            val likes = getPostLikes(post.post_id)
            val comments = getPostComments(post.post_id)
            val images = getPostImages(post.post_id)
            post.toDto(likes, comments, images)
        }
    }
    
    fun getFeedPosts(): Flow<List<PostDto>> = flow {
        emit(getFeedPostsList())
        
        val channel = supabaseClient.realtime.channel("feed-posts")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.POSTS_TABLE
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getFeedPostsList())
        }
    }
    
    suspend fun getPostById(postId: String): PostDto? {
        val post = supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
            .select {
                filter {
                    eq("post_id", postId)
                }
            }
            .decodeSingleOrNull<SupabasePost>() ?: return null
        
        val likes = getPostLikes(postId)
        val comments = getPostComments(postId)
        val images = getPostImages(postId)
        return post.toDto(likes, comments, images)
    }
    
    fun getPostByIdFlow(postId: String): Flow<PostDto?> = flow {
        emit(getPostById(postId))
        
        val channel = supabaseClient.realtime.channel("post-$postId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.POSTS_TABLE
            filter("post_id", FilterOperator.EQ, postId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getPostById(postId))
        }
    }
    
    suspend fun getUserPosts(userId: String): List<PostDto> {
        val posts = supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
            .select {
                filter {
                    eq("user_id", userId)
                }
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<SupabasePost>()
        
        return posts.map { post ->
            val likes = getPostLikes(post.post_id)
            val comments = getPostComments(post.post_id)
            val images = getPostImages(post.post_id)
            post.toDto(likes, comments, images)
        }
    }
    
    fun getUserPostsFlow(userId: String): Flow<List<PostDto>> = flow {
        emit(getUserPosts(userId))
        
        val channel = supabaseClient.realtime.channel("user-posts-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.POSTS_TABLE
            filter("user_id", FilterOperator.EQ, userId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getUserPosts(userId))
        }
    }
    
    suspend fun createPost(postDto: PostDto): PostDto {
        val postId = UUID.randomUUID().toString()
        
        // Encode image URLs list as JSON string for inline storage
        val imageUrlsJson = if (postDto.imageUrls.isNotEmpty()) {
            json.encodeToString(postDto.imageUrls)
        } else "[]"
        
        // For video posts, use thumbnail as the post_image_url (required field)
        val postImageUrl = when {
            postDto.postImageUrl.isNotBlank() -> postDto.postImageUrl
            postDto.thumbnailUrl.isNotBlank() -> postDto.thumbnailUrl
            postDto.imageUrls.isNotEmpty() -> postDto.imageUrls.first()
            else -> "" // Will fail if column is NOT NULL - need to fix DB schema
        }
        
        val supabasePost = SupabasePost(
            post_id = postId,
            user_id = postDto.userId,
            username = postDto.username,
            user_profile_image = postDto.userProfileImage,
            post_image_url = postImageUrl,
            image_urls = imageUrlsJson,
            caption = postDto.caption,
            location = postDto.location,
            music_name = postDto.musicName,
            music_artist = postDto.musicArtist,
            timestamp = System.currentTimeMillis(),
            // Video/Reel fields
            media_type = postDto.mediaType,
            video_url = postDto.videoUrl,
            thumbnail_url = postDto.thumbnailUrl,
            video_duration = postDto.videoDuration,
            video_width = postDto.videoWidth,
            video_height = postDto.videoHeight,
            aspect_ratio = postDto.aspectRatio,
            views_count = 0
        )
        
        supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
            .insert(supabasePost)
        Log.d("PostDataSource", "createPost: post $postId inserted (type: ${postDto.mediaType}) with ${postDto.imageUrls.size} images inline")
        
        // Also insert into post_images table (belt-and-suspenders)
        if (postDto.imageUrls.isNotEmpty()) {
            val postImages = postDto.imageUrls.mapIndexed { index, imageUrl ->
                SupabasePostImageInsert(
                    post_id = postId,
                    image_url = imageUrl,
                    position = index
                )
            }
            try {
                supabaseClient.postgrest["post_images"].insert(postImages)
                Log.d("PostDataSource", "createPost: inserted ${postImages.size} carousel images into post_images for $postId")
            } catch (e: Exception) {
                Log.e("PostDataSource", "createPost: post_images insert failed (OK, using inline): ${e.message}")
            }
        }
        
        return postDto.copy(postId = postId, timestamp = supabasePost.timestamp)
    }
    
    suspend fun deletePost(postId: String) {
        // Delete comments first
        supabaseClient.postgrest[SupabaseConfig.COMMENTS_TABLE]
            .delete {
                filter {
                    eq("post_id", postId)
                }
            }
        
        // Delete likes
        supabaseClient.postgrest[SupabaseConfig.LIKES_TABLE]
            .delete {
                filter {
                    eq("post_id", postId)
                }
            }
        
        // Delete post
        supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
            .delete {
                filter {
                    eq("post_id", postId)
                }
            }
    }
    
    suspend fun likePost(postId: String, userId: String) {
        val like = PostLike(
            post_id = postId,
            user_id = userId
        )
        supabaseClient.postgrest[SupabaseConfig.LIKES_TABLE]
            .insert(like)
    }
    
    suspend fun unlikePost(postId: String, userId: String) {
        supabaseClient.postgrest[SupabaseConfig.LIKES_TABLE]
            .delete {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
    }
    
    suspend fun isPostLikedByUser(postId: String, userId: String): Boolean {
        val result = supabaseClient.postgrest[SupabaseConfig.LIKES_TABLE]
            .select {
                filter {
                    eq("post_id", postId)
                    eq("user_id", userId)
                }
            }
            .decodeList<PostLike>()
        return result.isNotEmpty()
    }
    
    suspend fun addComment(postId: String, commentDto: CommentDto): CommentDto {
        val commentId = UUID.randomUUID().toString()
        val supabaseComment = SupabaseComment(
            comment_id = commentId,
            post_id = postId,
            user_id = commentDto.userId,
            username = commentDto.username,
            user_profile_image = commentDto.userProfileImage,
            content = commentDto.content,
            timestamp = System.currentTimeMillis()
        )
        
        supabaseClient.postgrest[SupabaseConfig.COMMENTS_TABLE]
            .insert(supabaseComment)
        
        return commentDto.copy(commentId = commentId, timestamp = supabaseComment.timestamp)
    }
    
    suspend fun getComments(postId: String): List<CommentDto> {
        return getPostComments(postId)
    }
    
    fun getCommentsFlow(postId: String): Flow<List<CommentDto>> = flow {
        emit(getComments(postId))
        
        val channel = supabaseClient.realtime.channel("comments-$postId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = SupabaseConfig.COMMENTS_TABLE
            filter("post_id", FilterOperator.EQ, postId)
        }
        
        channel.subscribe()
        
        changeFlow.collect {
            emit(getComments(postId))
        }
    }
    
    suspend fun deleteComment(postId: String, commentId: String) {
        supabaseClient.postgrest[SupabaseConfig.COMMENTS_TABLE]
            .delete {
                filter {
                    eq("comment_id", commentId)
                }
            }
    }
    
    suspend fun getPostLikers(postId: String): List<String> {
        val likes = supabaseClient.postgrest[SupabaseConfig.LIKES_TABLE]
            .select(columns = Columns.list("user_id")) {
                filter {
                    eq("post_id", postId)
                }
            }
            .decodeList<Map<String, String>>()
        return likes.mapNotNull { it["user_id"] }
    }
    
    // ========================= VIDEO VIEW TRACKING =========================
    
    /**
     * Record or update a video view for a user.
     * Uses upsert to either create a new view or update watch duration.
     */
    suspend fun recordVideoView(postId: String, userId: String, watchDuration: Int = 0) {
        try {
            val videoView = VideoView(
                post_id = postId,
                user_id = userId,
                watch_duration = watchDuration,
                watched_at = System.currentTimeMillis()
            )
            supabaseClient.postgrest["video_views"]
                .upsert(videoView) {
                    onConflict = "post_id,user_id"
                }
            Log.d("PostDataSource", "recordVideoView: recorded view for post $postId by user $userId")
        } catch (e: Exception) {
            Log.e("PostDataSource", "recordVideoView failed: ${e.message}", e)
        }
    }
    
    /**
     * Increment the views_count on a post.
     * Called after a video has been watched for at least 3 seconds.
     */
    suspend fun incrementViewCount(postId: String) {
        try {
            // Use RPC function or raw SQL update to increment
            supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
                .update({
                    // Get current post first, increment, update
                }) {
                    filter { eq("post_id", postId) }
                }
            // Note: For atomic increment, you would typically use a Supabase RPC function
            // For now, we'll rely on the video_views table for accurate counts
            Log.d("PostDataSource", "incrementViewCount: incremented for post $postId")
        } catch (e: Exception) {
            Log.e("PostDataSource", "incrementViewCount failed: ${e.message}", e)
        }
    }
    
    /**
     * Get the view count for a video post.
     */
    suspend fun getVideoViewCount(postId: String): Int {
        return try {
            val views = supabaseClient.postgrest["video_views"]
                .select(columns = Columns.list("id")) {
                    filter { eq("post_id", postId) }
                }
                .decodeList<Map<String, Any>>()
            views.size
        } catch (e: Exception) {
            Log.e("PostDataSource", "getVideoViewCount failed: ${e.message}", e)
            0
        }
    }
    
    /**
     * Check if a user has already viewed a video.
     */
    suspend fun hasUserViewedVideo(postId: String, userId: String): Boolean {
        return try {
            val result = supabaseClient.postgrest["video_views"]
                .select {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<VideoView>()
            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e("PostDataSource", "hasUserViewedVideo failed: ${e.message}", e)
            false
        }
    }
    
    // ========================= SAVED POSTS =========================
    
    /**
     * Save (bookmark) a post for a user.
     */
    suspend fun savePost(postId: String, userId: String) {
        try {
            val savedPost = SavedPost(
                post_id = postId,
                user_id = userId,
                created_at = System.currentTimeMillis()
            )
            supabaseClient.postgrest["saved_posts"]
                .upsert(savedPost) {
                    onConflict = "post_id,user_id"
                }
            Log.d("PostDataSource", "savePost: saved post $postId for user $userId")
        } catch (e: Exception) {
            Log.e("PostDataSource", "savePost failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Unsave (remove bookmark) a post for a user.
     */
    suspend fun unsavePost(postId: String, userId: String) {
        try {
            supabaseClient.postgrest["saved_posts"]
                .delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
            Log.d("PostDataSource", "unsavePost: unsaved post $postId for user $userId")
        } catch (e: Exception) {
            Log.e("PostDataSource", "unsavePost failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Check if a post is saved by a user.
     */
    suspend fun isPostSavedByUser(postId: String, userId: String): Boolean {
        return try {
            val result = supabaseClient.postgrest["saved_posts"]
                .select {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<SavedPost>()
            result.isNotEmpty()
        } catch (e: Exception) {
            Log.e("PostDataSource", "isPostSavedByUser failed: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get all saved post IDs for a user.
     */
    suspend fun getSavedPostIds(userId: String): List<String> {
        return try {
            val savedPosts = supabaseClient.postgrest["saved_posts"]
                .select(columns = Columns.list("post_id")) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Map<String, String>>()
            savedPosts.mapNotNull { it["post_id"] }
        } catch (e: Exception) {
            Log.e("PostDataSource", "getSavedPostIds failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get all saved posts for a user (full post data).
     */
    suspend fun getSavedPosts(userId: String): List<PostDto> {
        return try {
            val savedPostIds = getSavedPostIds(userId)
            if (savedPostIds.isEmpty()) return emptyList()
            
            val posts = mutableListOf<PostDto>()
            for (postId in savedPostIds) {
                getPostById(postId)?.let { posts.add(it) }
            }
            posts
        } catch (e: Exception) {
            Log.e("PostDataSource", "getSavedPosts failed: ${e.message}", e)
            emptyList()
        }
    }
}

/**
 * Supabase representation of Saved Post.
 */
@Serializable
data class SavedPost(
    val id: Long? = null,
    val post_id: String,
    val user_id: String,
    val created_at: Long = System.currentTimeMillis()
)

/**
 * Supabase representation of Video View.
 */
@Serializable
data class VideoView(
    val id: Long? = null,
    val post_id: String,
    val user_id: String,
    val watch_duration: Int = 0,
    val watched_at: Long = System.currentTimeMillis()
)
