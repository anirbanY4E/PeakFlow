package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.UserRepository

class GetCurrentUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): User? {
        return userRepository.getCurrentUser()
    }
}