package com.junaidjamshid.i211203.data.mapper

import com.junaidjamshid.i211203.data.dto.CommentDto
import com.junaidjamshid.i211203.data.dto.PostDto
import com.junaidjamshid.i211203.domain.model.Comment
import com.junaidjamshid.i211203.domain.model.Post

/**
 * Mapper functions for Post data conversion.
 */
object PostMapper {
    
    fun PostDto.toDomain(currentUserId: String): Post {
        return Post(
            postId = postId,
            userId = userId,
            username = username,
            userProfileImage = userProfileImage,
            postImageUrl = postImageUrl,
            caption = caption,
            timestamp = timestamp,
            likesCount = likes.size,
            commentsCount = comments.size,
            isLikedByCurrentUser = likes.containsKey(currentUserId),
            isSavedByCurrentUser = false // Will be calculated separately
        )
    }
    
    fun CommentDto.toDomain(currentUserId: String): Comment {
        return Comment(
            commentId = commentId,
            postId = postId,
            userId = userId,
            username = username,
            userProfileImage = userProfileImage,
            content = content,
            timestamp = timestamp,
            likesCount = likes.size,
            isLikedByCurrentUser = likes.containsKey(currentUserId)
        )
    }
    
    fun Comment.toDto(): CommentDto {
        return CommentDto(
            commentId = commentId,
            postId = postId,
            userId = userId,
            username = username,
            userProfileImage = userProfileImage,
            content = content,
            timestamp = timestamp
        )
    }
}
