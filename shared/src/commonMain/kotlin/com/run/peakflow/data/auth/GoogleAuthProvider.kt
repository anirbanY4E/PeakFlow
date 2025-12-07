package com.run.peakflow.data.auth

/**
 * Platform-specific Google authentication provider
 * Implementations handle the actual Google Sign-In flow for each platform
 */
interface GoogleAuthProvider {
    /**
     * Initiates Google Sign-In flow
     * @return GoogleAuthResult containing user info or error
     */
    suspend fun signIn(): GoogleAuthResult

    /**
     * Signs out from Google account
     */
    suspend fun signOut()
}

/**
 * Result of Google authentication
 */
sealed class GoogleAuthResult {
    data class Success(
        val idToken: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?
    ) : GoogleAuthResult()

    data class Error(val message: String) : GoogleAuthResult()
    data object Cancelled : GoogleAuthResult()
}
