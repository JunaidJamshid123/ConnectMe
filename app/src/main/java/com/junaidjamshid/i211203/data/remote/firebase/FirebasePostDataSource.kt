package com.junaidjamshid.i211203.data.remote.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.data.dto.CommentDto
import com.junaidjamshid.i211203.data.dto.PostDto
import com.junaidjamshid.i211203.util.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firebase Post operations.
 */
@Singleton
class FirebasePostDataSource @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) {
    
    private val postsRef = firebaseDatabase.reference.child(Constants.POSTS_REF)
    
    fun getFeedPosts(): Flow<List<PostDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children.mapNotNull { 
                    it.getValue(PostDto::class.java) 
                }.sortedByDescending { it.timestamp }
                trySend(posts)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        postsRef.addValueEventListener(listener)
        
        awaitClose {
            postsRef.removeEventListener(listener)
        }
    }
    
    suspend fun getPostById(postId: String): PostDto? {
        val snapshot = postsRef.child(postId).get().await()
        return snapshot.getValue(PostDto::class.java)
    }
    
    fun getPostByIdFlow(postId: String): Flow<PostDto?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.getValue(PostDto::class.java)
                trySend(post)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        postsRef.child(postId).addValueEventListener(listener)
        
        awaitClose {
            postsRef.child(postId).removeEventListener(listener)
        }
    }
    
    suspend fun getUserPosts(userId: String): List<PostDto> {
        val snapshot = postsRef.orderByChild("userId")
            .equalTo(userId)
            .get()
            .await()
        return snapshot.children.mapNotNull { it.getValue(PostDto::class.java) }
            .sortedByDescending { it.timestamp }
    }
    
    fun getUserPostsFlow(userId: String): Flow<List<PostDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = snapshot.children
                    .mapNotNull { it.getValue(PostDto::class.java) }
                    .filter { it.userId == userId }
                    .sortedByDescending { it.timestamp }
                trySend(posts)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        postsRef.addValueEventListener(listener)
        
        awaitClose {
            postsRef.removeEventListener(listener)
        }
    }
    
    suspend fun createPost(postDto: PostDto): PostDto {
        val postId = postsRef.push().key ?: throw Exception("Failed to generate post ID")
        val post = postDto.copy(postId = postId)
        postsRef.child(postId).setValue(post).await()
        return post
    }
    
    suspend fun deletePost(postId: String) {
        postsRef.child(postId).removeValue().await()
    }
    
    suspend fun likePost(postId: String, userId: String) {
        postsRef.child(postId).child("likes").child(userId).setValue(true).await()
    }
    
    suspend fun unlikePost(postId: String, userId: String) {
        postsRef.child(postId).child("likes").child(userId).removeValue().await()
    }
    
    suspend fun isPostLikedByUser(postId: String, userId: String): Boolean {
        val snapshot = postsRef.child(postId).child("likes").child(userId).get().await()
        return snapshot.exists()
    }
    
    suspend fun addComment(postId: String, commentDto: CommentDto): CommentDto {
        val commentId = postsRef.child(postId).child("comments").push().key 
            ?: throw Exception("Failed to generate comment ID")
        val comment = commentDto.copy(commentId = commentId)
        postsRef.child(postId).child("comments").child(commentId).setValue(comment).await()
        return comment
    }
    
    suspend fun getComments(postId: String): List<CommentDto> {
        val snapshot = postsRef.child(postId).child("comments").get().await()
        return snapshot.children.mapNotNull { it.getValue(CommentDto::class.java) }
            .sortedBy { it.timestamp }
    }
    
    fun getCommentsFlow(postId: String): Flow<List<CommentDto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children
                    .mapNotNull { it.getValue(CommentDto::class.java) }
                    .sortedBy { it.timestamp }
                trySend(comments)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        postsRef.child(postId).child("comments").addValueEventListener(listener)
        
        awaitClose {
            postsRef.child(postId).child("comments").removeEventListener(listener)
        }
    }
    
    suspend fun deleteComment(postId: String, commentId: String) {
        postsRef.child(postId).child("comments").child(commentId).removeValue().await()
    }
    
    suspend fun getPostLikers(postId: String): List<String> {
        val snapshot = postsRef.child(postId).child("likes").get().await()
        return snapshot.children.mapNotNull { it.key }
    }
}
