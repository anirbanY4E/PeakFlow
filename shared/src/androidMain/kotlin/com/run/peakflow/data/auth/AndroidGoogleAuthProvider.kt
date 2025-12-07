package com.run.peakflow.data.auth

import kotlinx.coroutines.delay

/**
 * Android implementation of Google Sign-In
 * For MVP: Simulates Google Sign-In flow
 *
 * TODO: For production, integrate with:
 * - Google Sign-In SDK: com.google.android.gms:play-services-auth
 * - Credential Manager API (Android 14+)
 */
class AndroidGoogleAuthProvider : GoogleAuthProvider {

    override suspend fun signIn(): GoogleAuthResult {
        return try {
            // Simulate network delay
            delay(1000)

            // For MVP, we'll simulate Google Sign-In success
            // In a real implementation, this would:
            // 1. Launch Google Sign-In intent
            // 2. Get ID token from Google
            // 3. Verify token
            // 4. Return user data
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
        // In real implementation: GoogleSignIn.getClient(context, options).signOut()
    }
}

