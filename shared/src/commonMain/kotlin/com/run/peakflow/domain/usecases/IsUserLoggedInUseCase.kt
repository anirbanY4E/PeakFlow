package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.UserRepository

class IsUserLoggedInUseCase(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Boolean {
        return userRepository.isLoggedIn()
    }
}