package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.AuthRepository

class ResendOtpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String): Result<Boolean> {
        return try {
            val success = authRepository.resendOtp(userId)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to resend OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}