package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke() {
        authRepository.logout()
    }
}