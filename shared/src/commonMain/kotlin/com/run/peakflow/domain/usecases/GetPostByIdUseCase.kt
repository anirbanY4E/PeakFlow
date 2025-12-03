package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Post
import com.run.peakflow.data.repository.PostRepository

class GetPostByIdUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String): Post? {
        return postRepository.getPostById(postId)
    }
}