package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.UserRepository

class CreateUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(name: String, city: String): User {
        return repository.createUser(name, city)
    }
}