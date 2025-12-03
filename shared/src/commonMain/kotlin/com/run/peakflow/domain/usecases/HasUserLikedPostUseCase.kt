package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.data.repository.UserRepository

class HasUserLikedPostUseCase(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(postId: String): Boolean {
        val userId = userRepository.getCurrentUserId() ?: return false
        return postRepository.hasUserLikedPost(postId, userId)
    }
}