package com.run.peakflow.data.repository

import com.run.peakflow.data.models.Post
import com.run.peakflow.data.models.PostComment
import com.run.peakflow.data.network.ApiService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class PostStateChange(
    val postId: String,
    val likeStatusChanged: Boolean = false,
    val commentsCountChanged: Boolean = false,
    val likesCountChanged: Boolean = false,
    val wasCreated: Boolean = false
)

class PostRepository(
    private val api: ApiService
) {
    private val _postStateChanges = MutableSharedFlow<PostStateChange>(replay = 0)
    val postStateChanges: SharedFlow<PostStateChange> = _postStateChanges.asSharedFlow()

    // ==================== POSTS ====================

    suspend fun getCommunityPosts(communityId: String): List<Post> {
        return api.getCommunityPosts(communityId)
    }

    suspend fun getFeedPosts(communityIds: List<String>): List<Post> {
        return api.getFeedPosts(communityIds)
    }

    suspend fun getPostById(postId: String): Post? {
        return api.getPostById(postId)
    }

    suspend fun createPost(
        communityId: String,
        authorId: String,
        content: String,
        imageUrl: String?
    ): Post {
        val post = api.createPost(communityId, authorId, content, imageUrl)
        // Signal that a new post was created
        _postStateChanges.emit(PostStateChange(postId = post.id, wasCreated = true))
        return post
    }

    suspend fun deletePost(postId: String): Boolean {
        return api.deletePost(postId)
    }

    suspend fun likePost(postId: String, userId: String): Boolean {
        val result = api.likePost(postId, userId)
        if (result) {
            _postStateChanges.emit(
                PostStateChange(
                    postId = postId,
                    likeStatusChanged = true,
                    likesCountChanged = true
                )
            )
        }
        return result
    }

    suspend fun unlikePost(postId: String, userId: String): Boolean {
        val result = api.unlikePost(postId, userId)
        if (result) {
            _postStateChanges.emit(
                PostStateChange(
                    postId = postId,
                    likeStatusChanged = true,
                    likesCountChanged = true
                )
            )
        }
        return result
    }

    suspend fun hasUserLikedPost(postId: String, userId: String): Boolean {
        return api.hasUserLikedPost(postId, userId)
    }

    // ==================== COMMENTS ====================

    suspend fun getPostComments(postId: String): List<PostComment> {
        return api.getPostComments(postId)
    }

    suspend fun addComment(
        postId: String,
        userId: String,
        content: String
    ): PostComment {
        val comment = api.addComment(postId, userId, content)
        _postStateChanges.emit(
            PostStateChange(
                postId = postId,
                commentsCountChanged = true
            )
        )
        return comment
    }

    suspend fun deleteComment(commentId: String): Boolean {
        return api.deleteComment(commentId)
    }
}
