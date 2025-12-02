package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.UserRepository

class IsUserLoggedInUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(): Boolean {
        return repository.isUserLoggedIn()
    }
}