package com.run.peakflow.data.repository

import com.run.peakflow.data.models.Post
import com.run.peakflow.data.models.PostComment
import com.run.peakflow.data.network.ApiService

class PostRepository(
    private val api: ApiService
) {
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
        return api.createPost(communityId, authorId, content, imageUrl)
    }

    suspend fun deletePost(postId: String): Boolean {
        return api.deletePost(postId)
    }

    suspend fun likePost(postId: String, userId: String): Boolean {
        return api.likePost(postId, userId)
    }

    suspend fun unlikePost(postId: String, userId: String): Boolean {
        return api.unlikePost(postId, userId)
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
        return api.addComment(postId, userId, content)
    }

    suspend fun deleteComment(commentId: String): Boolean {
        return api.deleteComment(commentId)
    }
}