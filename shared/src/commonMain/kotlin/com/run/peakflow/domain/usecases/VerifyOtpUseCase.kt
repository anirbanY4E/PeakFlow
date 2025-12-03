package com.run.peakflow.domain.usecases

import com.run.peakflow.data.repository.AuthRepository

class VerifyOtpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(userId: String, otp: String): Result<Boolean> {
        return try {
            if (otp.length != 6 || !otp.all { it.isDigit() }) {
                return Result.failure(Exception("OTP must be 6 digits"))
            }
            val success = authRepository.verifyOtp(userId, otp)
            if (success) {
                Result.success(true)
            } else {
                Result.failure(Exception("Invalid OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}