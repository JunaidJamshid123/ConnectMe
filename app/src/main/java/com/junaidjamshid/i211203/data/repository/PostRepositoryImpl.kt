package com.junaidjamshid.i211203.data.repository

import android.util.Base64
import com.junaidjamshid.i211203.data.dto.CommentDto
import com.junaidjamshid.i211203.data.dto.PostDto
import com.junaidjamshid.i211203.data.mapper.PostMapper
import com.junaidjamshid.i211203.data.mapper.PostMapper.toDomain
import com.junaidjamshid.i211203.data.mapper.PostMapper.toDto
import com.junaidjamshid.i211203.data.remote.supabase.SupabaseNotificationDataSource
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
    private val userDataSource: SupabaseUserDataSource,
    private val notificationDataSource: SupabaseNotificationDataSource
) : PostRepository {
    
    override fun getFeedPosts(userId: String): Flow<Resource<List<Post>>> {
        return postDataSource.getFeedPosts()
            .map { posts ->
                // Enrich posts with latest user profile images from the users table
                val enrichedPosts = enrichPostsWithUserData(posts)
                // Get saved post IDs for current user
                val savedPostIds = try {
                    postDataSource.getSavedPostIds(userId).toSet()
                } catch (e: Exception) {
                    emptySet()
                }
                Resource.Success(enrichedPosts.map { dto -> 
                    dto.toDomain(userId).copy(isSavedByCurrentUser = savedPostIds.contains(dto.postId))
                }) as Resource<List<Post>>
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
                val enrichedPosts = enrichPostsWithUserData(posts)
                Resource.Success(enrichedPosts.map { it.toDomain(userId) }) as Resource<List<Post>>
            }
            .catch { e ->
                emit(Resource.Error(e.message ?: "Failed to get user posts"))
            }
    }

    /**
     * Enrich posts with the latest user profile images from the users table.
     * Avoids showing stale or missing profile pictures that were baked in at post creation time.
     */
    private suspend fun enrichPostsWithUserData(posts: List<PostDto>): List<PostDto> {
        // Gather unique user IDs and batch-fetch their current profiles
        val userIds = posts.map { it.userId }.distinct()
        val userProfiles = mutableMapOf<String, String>() // userId -> profilePicture

        for (uid in userIds) {
            try {
                val user = userDataSource.getUserById(uid)
                user?.let {
                    userProfiles[uid] = it.profilePicture ?: ""
                }
            } catch (_: Exception) { /* keep stale value if lookup fails */ }
        }

        return posts.map { post ->
            val freshProfileImage = userProfiles[post.userId]
            if (freshProfileImage != null && freshProfileImage.isNotEmpty()) {
                post.copy(userProfileImage = freshProfileImage)
            } else {
                post
            }
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
    
    override suspend fun createPost(
        caption: String,
        imageBytesList: List<ByteArray>,
        location: String,
        musicName: String,
        musicArtist: String
    ): Resource<Post> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                val user = userDataSource.getUserById(currentUserId)
                val imageBase64List = imageBytesList.map { bytes ->
                    Base64.encodeToString(bytes, Base64.DEFAULT)
                }
                val primaryImage = imageBase64List.firstOrNull() ?: ""
                val postDto = PostDto(
                    postId = "",
                    userId = currentUserId,
                    username = user?.username ?: "",
                    userProfileImage = user?.profilePicture ?: "",
                    postImageUrl = primaryImage,
                    imageUrls = imageBase64List,
                    caption = caption,
                    location = location,
                    musicName = musicName,
                    musicArtist = musicArtist,
                    timestamp = System.currentTimeMillis()
                )
                val createdPost = postDataSource.createPost(postDto)
                Resource.Success(createdPost.toDomain(currentUserId))
            } else {
                Resource.Error("User not logged in")
            }
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
                
                // Create notification for post owner (if not self)
                createLikeNotification(postId, currentUserId)
                
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
            
            // Create notification for post owner (if not self)
            createLikeNotification(postId, userId)
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to like post")
        }
    }
    
    /**
     * Creates a like notification for the post owner.
     * Does not create notification if the liker is the post owner.
     */
    private suspend fun createLikeNotification(postId: String, likerId: String) {
        try {
            val post = postDataSource.getPostById(postId) ?: return
            
            // Don't notify self
            if (post.userId == likerId) return
            
            val liker = userDataSource.getUserById(likerId) ?: return
            
            notificationDataSource.createNotification(
                recipientId = post.userId,
                actorId = likerId,
                actorUsername = liker.username,
                actorProfileImage = liker.profilePicture ?: "",
                type = "like",
                postId = postId,
                postThumbnail = post.postImageUrl
            )
        } catch (_: Exception) {
            // Silently fail - don't break the like operation
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
        return try {
            postDataSource.savePost(postId, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save post")
        }
    }
    
    override suspend fun unsavePost(postId: String, userId: String): Resource<Unit> {
        return try {
            postDataSource.unsavePost(postId, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to unsave post")
        }
    }
    
    override suspend fun getSavedPosts(userId: String): Resource<List<Post>> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId() ?: userId
            val savedPostDtos = postDataSource.getSavedPosts(userId)
            val savedPostIds = postDataSource.getSavedPostIds(currentUserId)
            val posts = savedPostDtos.map { dto -> 
                PostMapper.run {
                    dto.toDomain(currentUserId).copy(
                        isSavedByCurrentUser = savedPostIds.contains(dto.postId)
                    )
                }
            }
            Resource.Success(posts)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get saved posts")
        }
    }
    
    override suspend fun addComment(postId: String, commentText: String): Resource<Comment> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                val user = userDataSource.getUserById(currentUserId)
                val commentId = UUID.randomUUID().toString()
                val commentDto = CommentDto(
                    commentId = commentId,
                    userId = currentUserId,
                    username = user?.username ?: "",
                    userProfileImage = user?.profilePicture ?: "",
                    content = commentText,
                    timestamp = System.currentTimeMillis()
                )
                val createdComment = postDataSource.addComment(postId, commentDto)
                
                // Create notification for post owner (if not self)
                createCommentNotification(postId, currentUserId, commentId, commentText)
                
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
            
            // Create notification for post owner (if not self)
            createCommentNotification(postId, comment.userId, comment.commentId, comment.content)
            
            Resource.Success(createdComment.toDomain(""))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add comment")
        }
    }
    
    /**
     * Creates a comment notification for the post owner.
     * Does not create notification if the commenter is the post owner.
     */
    private suspend fun createCommentNotification(
        postId: String,
        commenterId: String,
        commentId: String,
        commentText: String
    ) {
        try {
            val post = postDataSource.getPostById(postId) ?: return
            
            // Don't notify self
            if (post.userId == commenterId) return
            
            val commenter = userDataSource.getUserById(commenterId) ?: return
            
            notificationDataSource.createNotification(
                recipientId = post.userId,
                actorId = commenterId,
                actorUsername = commenter.username,
                actorProfileImage = commenter.profilePicture ?: "",
                type = "comment",
                postId = postId,
                postThumbnail = post.postImageUrl,
                commentId = commentId,
                commentText = commentText.take(100) // Limit to 100 chars
            )
        } catch (_: Exception) {
            // Silently fail - don't break the comment operation
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
    
    // ========================= VIDEO METHODS =========================
    
    override suspend fun recordVideoView(postId: String, userId: String): Resource<Unit> {
        return try {
            postDataSource.recordVideoView(postId, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to record video view")
        }
    }
    
    override suspend fun getVideoViewCount(postId: String): Resource<Int> {
        return try {
            val count = postDataSource.getVideoViewCount(postId)
            Resource.Success(count)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get video view count")
        }
    }
    
    override suspend fun createVideoPost(
        caption: String,
        videoUrl: String,
        thumbnailUrl: String,
        videoDuration: Int,
        videoWidth: Int,
        videoHeight: Int,
        location: String,
        musicName: String,
        musicArtist: String,
        isReel: Boolean
    ): Resource<Post> {
        return try {
            val currentUserId = userDataSource.getCurrentUserId()
            if (currentUserId != null) {
                val user = userDataSource.getUserById(currentUserId)
                
                val postDto = PostDto(
                    userId = currentUserId,
                    username = user?.username ?: "",
                    userProfileImage = user?.profilePicture ?: "",
                    caption = caption,
                    location = location,
                    musicName = musicName,
                    musicArtist = musicArtist,
                    // Video-specific fields
                    mediaType = if (isReel) "reel" else "video",
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl,
                    videoDuration = videoDuration,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    aspectRatio = if (videoWidth > 0) videoHeight.toFloat() / videoWidth else 1f,
                    viewsCount = 0
                )
                
                val createdPost = postDataSource.createPost(postDto)
                Resource.Success(createdPost.toDomain(currentUserId))
            } else {
                Resource.Error("User not logged in")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create video post")
        }
    }
}
