package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.AuthRepository
import com.run.peakflow.data.repository.UserRepository

class CompleteProfileUseCase(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        city: String,
        interests: List<EventCategory>,
        avatarUrl: String? = null
    ): Result<User> {
        return try {
            val userId = userRepository.getCurrentUserId()
                ?: return Result.failure(Exception("User not logged in"))

            if (name.isBlank() || name.length < 2) {
                return Result.failure(Exception("Name must be at least 2 characters"))
            }

            val user = userRepository.completeProfile(userId, name, city, interests, avatarUrl)
            authRepository.updateAuthState(hasCompletedOnboarding = true)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}