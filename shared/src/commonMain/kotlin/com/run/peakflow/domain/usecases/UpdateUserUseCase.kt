package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.UserRepository

class UpdateUserUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        user: User,
        avatarBytes: ByteArray? = null
    ): Result<User> {
        return try {
            val updatedUser = userRepository.updateUser(user, avatarBytes)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}