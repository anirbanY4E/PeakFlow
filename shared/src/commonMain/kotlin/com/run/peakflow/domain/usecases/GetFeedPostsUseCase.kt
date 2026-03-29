package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Post
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.data.repository.UserRepository

class GetFeedPostsUseCase(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<Post> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        // The get_user_feed RPC already joins on memberships internally,
        // so we don't need to fetch memberships client-side.
        return postRepository.getFeedPosts(emptyList())
    }
}