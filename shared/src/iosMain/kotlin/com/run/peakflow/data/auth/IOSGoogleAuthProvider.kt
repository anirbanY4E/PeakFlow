package com.run.peakflow.data.auth

import com.run.peakflow.data.network.ApiService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS implementation of Google Sign-In
 * For iOS, we use the fallback browser flow via Supabase natively.
 */
class IOSGoogleAuthProvider : GoogleAuthProvider, KoinComponent {

    private val apiService: ApiService by inject()

    override suspend fun signIn(): GoogleAuthResult {
        return try {
            // Initiate the ASWebAuthenticationSession via Supabase-KT
            apiService.startGoogleBrowserOAuth()
            
            // Return that browser flow has been triggered.
            // The deeplink handler in AppDelegate/SwiftUI will capture the callback URL
            // and pass it to Supabase client, which will automatically update the session state.
            GoogleAuthResult.BrowserFlow
        } catch (e: Exception) {
            GoogleAuthResult.Error(e.message ?: "Google Sign-In failed")
        }
    }

    override suspend fun signOut() {
        // Handled centrally by API service logout()
    }
}
