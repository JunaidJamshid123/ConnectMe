package com.junaidjamshid.i211203.data.repository

import android.util.Base64
import com.junaidjamshid.i211203.data.dto.CommentDto
import com.junaidjamshid.i211203.data.dto.PostDto
import com.junaidjamshid.i211203.data.mapper.PostMapper.toDomain
import com.junaidjamshid.i211203.data.mapper.PostMapper.toDto
import com.junaidjamshid.i211203.data.remote.supabase.SupabasePostDataSource
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseUserDataSource
import com.junaidjamshid.i211203.domain.model.Comment
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.domain.repository.PostRepository
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PostRepository that uses Supabase.
 */
@Singleton
class PostRepositoryImpl @Inject constructor(
    private val postDataSource: SupabasePostDataSource,
    private val userDataSource: SupabaseUserDataSource
) : PostRepository {
    
    override fun getFeedPosts(userId: String): Flow<Resource<List<Post>>> {
        return postDataSource.getFeedPosts()
            .map { posts ->
                Resource.Success(posts.map { it.toDomain(userId) }) as Resource<List<Post>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to load posts"))
            }
    }
    
    override fun getPost(postId: String): Flow<Resource<Post>> {
        return postDataSource.getPostByIdFlow(postId)
            .map { postDto ->
                if (postDto != null) {
                    Resource.Success(postDto.toDomain("")) as Resource<Post>
                } else {
                    Resource.Error("Post not found")
                }
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to get post"))
            }
    }
    
    override suspend fun getPostById(postId: String): Resource<Post> {
        return try {
            val postDto = postDataSource.getPostById(postId)
            if (postDto != null) {
                Resource.Success(postDto.toDomain(""))
            } else {
                Resource.Error("Post not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get post")
        }
    }
    
    override fun getUserPosts(userId: String): Flow<Resource<List<Post>>> {
        return postDataSource.getUserPostsFlow(userId)
            .map { posts ->
                Resource.Success(posts.map { it.toDomain(userId) }) as Resource<List<Post>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to get user posts"))
            }
    }
    
    override suspend fun createPost(caption: String, imageBytes: ByteArray): Resource<Post> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                val imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                createPost(currentUserId, caption, imageBase64)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create post")
        }
    }
    
    override suspend fun createPost(
        userId: String,
        caption: String,
        imageBase64: String
    ): Resource<Post> {
        return try {
            val user = userDataSource.getUserById(userId)
            val postDto = PostDto(
                postId = "",
                userId = userId,
                username = user?.username ?: "",
                userProfileImage = user?.profilePicture ?: "",
                postImageUrl = imageBase64,
                caption = caption,
                timestamp = System.currentTimeMillis()
            )
            val createdPost = postDataSource.createPost(postDto)
            Resource.Success(createdPost.toDomain(userId))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create post")
        }
    }
    
    override suspend fun deletePost(postId: String): Resource<Unit> {
        return try {
            postDataSource.deletePost(postId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete post")
        }
    }
    
    override suspend fun likePost(postId: String): Resource<Unit> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                postDataSource.likePost(postId, currentUserId)
                Resource.Success(Unit)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to like post")
        }
    }
    
    override suspend fun unlikePost(postId: String): Resource<Unit> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                postDataSource.unlikePost(postId, currentUserId)
                Resource.Success(Unit)
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unlike post")
        }
    }
    
    override suspend fun likePost(postId: String, userId: String): Resource<Unit> {
        return try {
            postDataSource.likePost(postId, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to like post")
        }
    }
    
    override suspend fun unlikePost(postId: String, userId: String): Resource<Unit> {
        return try {
            postDataSource.unlikePost(postId, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unlike post")
        }
    }
    
    override suspend fun isPostLikedByUser(postId: String, userId: String): Resource<Boolean> {
        return try {
            val isLiked = postDataSource.isPostLikedByUser(postId, userId)
            Resource.Success(isLiked)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to check like status")
        }
    }
    
    override suspend fun savePost(postId: String, userId: String): Resource<Unit> {
        // TODO: Implement saved posts feature
        return Resource.Success(Unit)
    }
    
    override suspend fun unsavePost(postId: String, userId: String): Resource<Unit> {
        // TODO: Implement saved posts feature
        return Resource.Success(Unit)
    }
    
    override suspend fun getSavedPosts(userId: String): Resource<List<Post>> {
        // TODO: Implement saved posts feature
        return Resource.Success(emptyList())
    }
    
    override suspend fun addComment(postId: String, commentText: String): Resource<Comment> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                val user = userDataSource.getUserById(currentUserId)
                val commentDto = CommentDto(
                    commentId = UUID.randomUUID().toString(),
                    userId = currentUserId,
                    username = user?.username ?: "",
                    userProfileImage = user?.profilePicture ?: "",
                    content = commentText,
                    timestamp = System.currentTimeMillis()
                )
                val createdComment = postDataSource.addComment(postId, commentDto)
                Resource.Success(createdComment.toDomain(""))
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add comment")
        }
    }
    
    override suspend fun addComment(postId: String, comment: Comment): Resource<Comment> {
        return try {
            val commentDto = comment.toDto()
            val createdComment = postDataSource.addComment(postId, commentDto)
            Resource.Success(createdComment.toDomain(""))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add comment")
        }
    }
    
    override fun getComments(postId: String): Flow<Resource<List<Comment>>> {
        return postDataSource.getCommentsFlow(postId)
            .map { comments ->
                Resource.Success(comments.map { it.toDomain("") }) as Resource<List<Comment>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to get comments"))
            }
    }
    
    override suspend fun deleteComment(postId: String, commentId: String): Resource<Unit> {
        return try {
            postDataSource.deleteComment(postId, commentId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete comment")
        }
    }
    
    override suspend fun getPostLikers(postId: String): Resource<List<String>> {
        return try {
            val likers = postDataSource.getPostLikers(postId)
            Resource.Success(likers)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get likers")
        }
    }
}
