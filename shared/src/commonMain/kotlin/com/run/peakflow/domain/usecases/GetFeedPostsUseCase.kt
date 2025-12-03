package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Post
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.data.repository.UserRepository

class GetFeedPostsUseCase(
    private val postRepository: PostRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<Post> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        val memberships = membershipRepository.getUserMemberships(userId)
        val communityIds = memberships.map { it.communityId }
        return postRepository.getFeedPosts(communityIds)
    }
}