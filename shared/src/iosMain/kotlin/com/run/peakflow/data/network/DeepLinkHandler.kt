package com.run.peakflow.data.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Handles deep link URLs from iOS by forwarding them to Supabase Auth.
 * This must live in the shared module where supabase-kt dependencies are available.
 *
 * For PKCE flow: extracts the "code" query parameter and exchanges it for a session.
 * For implicit flow: parses the session directly from the URL fragment.
 */
fun handleSupabaseDeepLink(supabaseClient: SupabaseClient, urlStr: String) {
    // Use a SupervisorJob + Default dispatcher to avoid Kotlin/Native main-thread
    // coroutine issues when called from ObjC bridge
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        try {
            println("Deep link: Received URL: $urlStr")
            val url = Url(urlStr)
            // PKCE flow: look for an authorization code in the query parameters
            val code = url.parameters["code"]
            if (code != null) {
                println("Deep link: Found authorization code, exchanging for session...")
                supabaseClient.auth.exchangeCodeForSession(code)
                println("Deep link: Successfully exchanged code for session")
            } else {
                // Implicit flow fallback: parse session from URL fragment
                println("Deep link: No code found, trying implicit flow...")
                val session = supabaseClient.auth.parseSessionFromUrl(urlStr)
                supabaseClient.auth.importSession(session)
                println("Deep link: Successfully imported session from URL")
            }
        } catch (e: Exception) {
            println("Error handling deep link: ${e.message}")
            e.printStackTrace()
        }
    }
}

