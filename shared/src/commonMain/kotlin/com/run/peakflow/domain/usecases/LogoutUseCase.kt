package com.run.peakflow.domain.usecases

import com.run.peakflow.data.auth.GoogleAuthProvider
import com.run.peakflow.data.repository.AuthRepository

/**
 * Logs out the current user.
 *
 * Performs a full two-phase logout:
 *  1. Clears the platform-level credential state ([GoogleAuthProvider.signOut]) so that
 *     the next Google sign-in shows a fresh account picker instead of auto-selecting
 *     the now-invalid cached credential.
 *  2. Signs out from Supabase and clears the local session ([AuthRepository.logout]).
 *
 * Without step 1, users see "credential not available" on the next Google sign-in
 * attempt because CredentialManager still holds the stale credential.
 */
class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val googleAuthProvider: GoogleAuthProvider
) {
    suspend operator fun invoke() {
        // 1. Clear CredentialManager state first (platform-level).
        //    This is safe to call even when the user signed in with email/password.
        try {
            googleAuthProvider.signOut()
        } catch (_: Exception) {
            // Non-fatal — proceed with Supabase sign-out regardless.
        }

        // 2. Sign out from Supabase and clear in-memory session.
        authRepository.logout()
    }
}