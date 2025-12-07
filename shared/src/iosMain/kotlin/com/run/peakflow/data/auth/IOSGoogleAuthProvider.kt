package com.run.peakflow.data.auth

import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * iOS implementation of Google Sign-In
 * For MVP: Simulates Google Sign-In flow
 *
 * TODO: For production, integrate with:
 * - Google Sign-In SDK for iOS
 * - Sign in with Apple (Apple requirement)
 */
class IOSGoogleAuthProvider : GoogleAuthProvider {

    override suspend fun signIn(): GoogleAuthResult {
        return try {
            // Simulate network delay
            delay(1000)

            // For MVP, we'll simulate Google Sign-In success
            // In a real implementation, this would:
            // 1. Present Google Sign-In view controller
            // 2. Get ID token from Google
            // 3. Verify token
            // 4. Return user data
            val randomId = Random.nextInt(1000, 9999)
            GoogleAuthResult.Success(
                idToken = "mock_google_token_ios_$randomId",
                email = "google.user$randomId@gmail.com",
                displayName = "Google User",
                photoUrl = "https://i.pravatar.cc/150?img=${Random.nextInt(1, 70)}"
            )
        } catch (e: Exception) {
            GoogleAuthResult.Error(e.message ?: "Google Sign-In failed")
        }
    }

    override suspend fun signOut() {
        // For MVP, no-op
        // In real implementation: Call Google Sign-In SDK signOut
    }
}
