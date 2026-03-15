package com.run.peakflow.data.auth

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.run.peakflow.utils.ActivityProvider
import kotlinx.coroutines.CancellationException

/**
 * Android implementation of Google Sign-In using Credential Manager (Native One Tap)
 */
class AndroidGoogleAuthProvider : GoogleAuthProvider {

    override suspend fun signIn(): GoogleAuthResult {
        val activity = ActivityProvider.currentActivity
            ?: return GoogleAuthResult.Error("Activity context not available")

        val credentialManager = CredentialManager.create(activity)

        // TODO: The web client ID should be injected from BuildConfig/Credentials
        // For now, place a placeholder that the developer will replace with their Web Client ID
        // from the Google Cloud Console.
        val webClientId = "878219945232-vavln7088r6csdikj4f930mg38bsf9bm.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activity,
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                
                GoogleAuthResult.Success(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id, // usually the email or ID
                    displayName = googleIdTokenCredential.displayName,
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                )
            } else {
                Log.w("GoogleAuth", "Unexpected credential type: ${credential.type}")
                GoogleAuthResult.Error("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleAuthResult.Cancelled
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Sign-in failed", e)
            GoogleAuthResult.Error(e.message ?: "Sign-in failed")
        }
    }

    override suspend fun signOut() {
        // CredentialManager doesn't explicitly sign out, it clears when the user asks or token invalidates.
        val activity = ActivityProvider.currentActivity ?: return
        val credentialManager = CredentialManager.create(activity)
        try {
            credentialManager.clearCredentialState(
                androidx.credentials.ClearCredentialStateRequest()
            )
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Sign-out failed", e)
        }
    }
}
