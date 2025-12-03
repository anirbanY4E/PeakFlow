package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.models.Post
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.data.repository.UserRepository

class CreatePostUseCase(
    private val postRepository: PostRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        communityId: String,
        content: String,
        imageUrl: String? = null
    ): Result<Post> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            // Check if user is admin
            val membership = membershipRepository.getMembershipRole(userId, communityId)
            if (membership?.role != MembershipRole.ADMIN) {
                return Result.failure(Exception("Only admins can create posts"))
            }

            if (content.isBlank()) {
                return Result.failure(Exception("Post content cannot be empty"))
            }

            val post = postRepository.createPost(communityId, userId, content, imageUrl)
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}