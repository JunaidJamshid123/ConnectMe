package com.junaidjamshid.i211203.data.remote.supabase

import com.junaidjamshid.i211203.data.dto.CommentDto
import com.junaidjamshid.i211203.data.dto.PostDto
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
 * Supabase representation of Post.
 */
@Serializable
data class SupabasePost(
    val post_id: String = "",
    val user_id: String = "",
    val username: String = "",
    val user_profile_image: String = "",
    val post_image_url: String = "",
    val caption: String = "",
    val timestamp: Long = 0
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

fun SupabasePost.toDto(likes: Map<String, Boolean> = emptyMap(), comments: List<CommentDto> = emptyList()): PostDto = PostDto(
    postId = post_id,
    userId = user_id,
    username = username,
    userProfileImage = user_profile_image,
    postImageUrl = post_image_url,
    caption = caption,
    timestamp = timestamp,
    likes = likes.toMutableMap(),
    comments = comments.toMutableList()
)

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
    
    suspend fun getFeedPostsList(): List<PostDto> {
        val posts = supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
            .select {
                order("timestamp", Order.DESCENDING)
            }
            .decodeList<SupabasePost>()
        
        return posts.map { post ->
            val likes = getPostLikes(post.post_id)
            val comments = getPostComments(post.post_id)
            post.toDto(likes, comments)
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
        return post.toDto(likes, comments)
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
            post.toDto(likes, comments)
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
        val supabasePost = SupabasePost(
            post_id = postId,
            user_id = postDto.userId,
            username = postDto.username,
            user_profile_image = postDto.userProfileImage,
            post_image_url = postDto.postImageUrl,
            caption = postDto.caption,
            timestamp = System.currentTimeMillis()
        )
        
        supabaseClient.postgrest[SupabaseConfig.POSTS_TABLE]
            .insert(supabasePost)
        
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
}
