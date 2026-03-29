package com.run.peakflow.data.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.run.peakflow.utils.ActivityProvider
import kotlinx.coroutines.CancellationException

/**
 * Android implementation of Google Sign-In using Credential Manager (Native One Tap).
 *
 * Uses the recommended two-step pattern:
 *  1. Try a fast, frictionless sign-in with previously authorized accounts.
 *  2. On [NoCredentialException] fall back to the full account picker so the user
 *     can explicitly choose or add a Google account.
 *
 * This prevents the "credential not available" error that occurs after calling
 * [signOut], because the first attempt will quickly fail and the picker will
 * appear instead of the OS trying to auto-select a now-invalid credential.
 */
class AndroidGoogleAuthProvider : GoogleAuthProvider {

    // The Web Client ID from Google Cloud Console (OAuth 2.0 client for Web Application type).
    // TODO: move to BuildConfig or a secrets management strategy in production.
    private val webClientId =
        "878219945232-vavln7088r6csdikj4f930mg38bsf9bm.apps.googleusercontent.com"

    override suspend fun signIn(): GoogleAuthResult {
        val activity = ActivityProvider.currentActivity
            ?: return GoogleAuthResult.Error("Activity context not available")

        val credentialManager = CredentialManager.create(activity)

        // ── Step 1: Fast-path — only accounts already authorized for this app ──────────
        val fastPathOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)   // only previously-used accounts
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)             // auto-pick if exactly one match
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = GetCredentialRequest.Builder()
                    .addCredentialOption(fastPathOption)
                    .build(),
                context = activity
            )
            handleCredentialResponse(result)
        } catch (e: NoCredentialException) {
            // No previously-authorized account — fall through to the account picker.
            Log.d("GoogleAuth", "No authorized credential, showing full picker: ${e.message}")
            signInWithPicker(activity, credentialManager)
        } catch (e: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: CancellationException) {
            throw e // Must rethrow coroutine cancellation
        } catch (e: Exception) {
            // Any other error on fast-path → try the picker before giving up.
            Log.w("GoogleAuth", "Fast-path failed, falling back to picker", e)
            signInWithPicker(activity, credentialManager)
        }
    }

    /**
     * Step 2: Show the full account picker (all Google accounts on device, including
     * accounts that haven't been used for this app before).
     */
    private suspend fun signInWithPicker(
        activity: Activity,
        credentialManager: CredentialManager
    ): GoogleAuthResult {
        val pickerOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // show ALL Google accounts on device
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)            // always show picker, never auto-pick
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = GetCredentialRequest.Builder()
                    .addCredentialOption(pickerOption)
                    .build(),
                context = activity
            )
            handleCredentialResponse(result)
        } catch (e: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Picker sign-in failed", e)
            GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        }
    }

    private fun handleCredentialResponse(response: GetCredentialResponse): GoogleAuthResult {
        val credential = response.credential
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAuthResult.Success(
                idToken = googleIdTokenCredential.idToken,
                email = googleIdTokenCredential.id,
                displayName = googleIdTokenCredential.displayName,
                photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
            )
        } else {
            Log.w("GoogleAuth", "Unexpected credential type: ${credential.type}")
            GoogleAuthResult.Error("Unexpected credential type: ${credential.type}")
        }
    }

    override suspend fun signOut() {
        val activity = ActivityProvider.currentActivity ?: return
        val credentialManager = CredentialManager.create(activity)
        try {
            // Notify CredentialManager to drop any cached session for this app.
            // This is REQUIRED so the next signIn() attempt doesn't try to reuse
            // the now-invalidated credential and throw NoCredentialException.
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (e: Exception) {
            Log.e("GoogleAuth", "clearCredentialState failed (non-fatal)", e)
        }
    }
}
