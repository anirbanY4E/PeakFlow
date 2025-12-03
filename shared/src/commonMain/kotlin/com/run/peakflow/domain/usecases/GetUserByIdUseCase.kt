package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.UserRepository

class GetUserByIdUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String): User? {
        return userRepository.getUser(userId)
    }
}