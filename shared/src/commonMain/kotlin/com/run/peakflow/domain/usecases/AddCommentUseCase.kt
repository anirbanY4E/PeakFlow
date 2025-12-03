package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.PostComment
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.data.repository.UserRepository

class AddCommentUseCase(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(postId: String, content: String): Result<PostComment> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            if (content.isBlank()) {
                return Result.failure(Exception("Comment cannot be empty"))
            }

            val comment = postRepository.addComment(postId, userId, content)
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}