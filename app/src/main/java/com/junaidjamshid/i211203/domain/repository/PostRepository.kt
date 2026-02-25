package com.junaidjamshid.i211203.domain.repository

import com.junaidjamshid.i211203.domain.model.Comment
import com.junaidjamshid.i211203.domain.model.Post
import com.junaidjamshid.i211203.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for post-related operations.
 */
interface PostRepository {
    
    fun getFeedPosts(userId: String): Flow<Resource<List<Post>>>
    
    fun getPost(postId: String): Flow<Resource<Post>>
    
    suspend fun getPostById(postId: String): Resource<Post>
    
    fun getUserPosts(userId: String): Flow<Resource<List<Post>>>
    
    suspend fun createPost(caption: String, imageBytes: ByteArray): Resource<Post>
    
    suspend fun createPost(
        userId: String,
        caption: String,
        imageBase64: String
    ): Resource<Post>
    
    suspend fun deletePost(postId: String): Resource<Unit>
    
    suspend fun likePost(postId: String): Resource<Unit>
    
    suspend fun unlikePost(postId: String): Resource<Unit>
    
    suspend fun likePost(postId: String, userId: String): Resource<Unit>
    
    suspend fun unlikePost(postId: String, userId: String): Resource<Unit>
    
    suspend fun isPostLikedByUser(postId: String, userId: String): Resource<Boolean>
    
    suspend fun savePost(postId: String, userId: String): Resource<Unit>
    
    suspend fun unsavePost(postId: String, userId: String): Resource<Unit>
    
    suspend fun getSavedPosts(userId: String): Resource<List<Post>>
    
    suspend fun addComment(postId: String, commentText: String): Resource<Comment>
    
    suspend fun addComment(postId: String, comment: Comment): Resource<Comment>
    
    fun getComments(postId: String): Flow<Resource<List<Comment>>>
    
    suspend fun deleteComment(postId: String, commentId: String): Resource<Unit>
    
    suspend fun getPostLikers(postId: String): Resource<List<String>>
}
