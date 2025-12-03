package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.AuthRepository

class SignInUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        emailOrPhone: String,
        password: String
    ): Result<User> {
        return try {
            if (emailOrPhone.isBlank()) {
                return Result.failure(Exception("Email or phone is required"))
            }
            if (password.isBlank()) {
                return Result.failure(Exception("Password is required"))
            }
            val user = authRepository.signIn(emailOrPhone, password)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}