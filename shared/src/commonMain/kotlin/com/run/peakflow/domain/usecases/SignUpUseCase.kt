package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.AuthRepository

class SignUpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String?,
        phone: String?,
        password: String
    ): Result<User> {
        return try {
            if (email.isNullOrBlank() && phone.isNullOrBlank()) {
                return Result.failure(Exception("Email or phone is required"))
            }
            if (password.length < 8) {
                return Result.failure(Exception("Password must be at least 8 characters"))
            }
            val user = authRepository.signUp(email, phone, password)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}