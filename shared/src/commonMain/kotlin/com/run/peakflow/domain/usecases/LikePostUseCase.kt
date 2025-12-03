package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.data.repository.UserRepository

class LikePostUseCase(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(postId: String): Result<Boolean> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            val hasLiked = postRepository.hasUserLikedPost(postId, userId)
            if (hasLiked) {
                postRepository.unlikePost(postId, userId)
            } else {
                postRepository.likePost(postId, userId)
            }
            Result.success(!hasLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}