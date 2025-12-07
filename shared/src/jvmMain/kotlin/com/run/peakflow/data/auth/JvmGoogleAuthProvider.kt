package com.run.peakflow.data.auth

import kotlinx.coroutines.delay

/**
 * JVM implementation of Google Sign-In
 * For MVP: Simulates Google Sign-In flow
 * Used by the server module
 */
class JvmGoogleAuthProvider : GoogleAuthProvider {

    override suspend fun signIn(): GoogleAuthResult {
        return try {
            // Simulate network delay
            delay(1000)

            // For MVP, simulate Google Sign-In
            GoogleAuthResult.Success(
                idToken = "mock_google_token_${System.currentTimeMillis()}",
                email = "google.user${System.currentTimeMillis() % 1000}@gmail.com",
                displayName = "Google User",
                photoUrl = "https://i.pravatar.cc/150?img=${(1..70).random()}"
            )
        } catch (e: Exception) {
            GoogleAuthResult.Error(e.message ?: "Google Sign-In failed")
        }
    }

    override suspend fun signOut() {
        // For MVP, no-op
    }
}
