package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.PostComment
import com.run.peakflow.data.repository.PostRepository

class GetPostCommentsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String): List<PostComment> {
        return postRepository.getPostComments(postId)
    }
}