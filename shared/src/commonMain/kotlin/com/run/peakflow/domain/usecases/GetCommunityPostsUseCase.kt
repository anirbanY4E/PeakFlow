package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Post
import com.run.peakflow.data.repository.PostRepository

class GetCommunityPostsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(communityId: String, limit: Int = 20, offset: Int = 0): List<Post> {
        return postRepository.getCommunityPosts(communityId, limit, offset)
    }
}