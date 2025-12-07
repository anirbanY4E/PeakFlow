package com.run.peakflow.domain.usecases

import com.run.peakflow.data.auth.GoogleAuthProvider
import com.run.peakflow.data.auth.GoogleAuthResult
import com.run.peakflow.data.models.User
import com.run.peakflow.data.repository.AuthRepository

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
    private val googleAuthProvider: GoogleAuthProvider
) {
    suspend operator fun invoke(): Result<User> {
        return try {
            when (val result = googleAuthProvider.signIn()) {
                is GoogleAuthResult.Success -> {
                    val user = authRepository.signInWithGoogle(
                        idToken = result.idToken,
                        email = result.email,
                        displayName = result.displayName,
                        photoUrl = result.photoUrl
                    )
                    Result.success(user)
                }

                is GoogleAuthResult.Error -> {
                    Result.failure(Exception(result.message))
                }

                is GoogleAuthResult.Cancelled -> {
                    Result.failure(Exception("Sign-in cancelled"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
