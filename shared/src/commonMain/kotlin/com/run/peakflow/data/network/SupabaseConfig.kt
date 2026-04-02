package com.run.peakflow.data.network

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds



object SupabaseConfig {

    const val SUPABASE_URL = "https://ozqytemfworgzbbztnga.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im96cXl0ZW1md29yZ3piYnp0bmdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMzMDQ3MjYsImV4cCI6MjA4ODg4MDcyNn0.qj25P_kCkW53MRXjFeXeaLNkspk5khESoIw5-lClrNQ"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(kotlinx.serialization.json.Json {
            isLenient = true
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
        })
        install(Auth) {
            // Enable automatic session persistence and loading
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
            flowType = FlowType.PKCE
            // Deep link configuration for iOS/Android OAuth callbacks
            scheme = "com.run.peakflow"
            host = "login-callback"
        }
        install(Postgrest)
        install(Storage)
        install(Functions)
        install(Realtime) {
            // Connect only when a channel is subscribed (lazy),
            // not eagerly on app startup
            reconnectDelay = 5.seconds // Less aggressive reconnection
        }
    }
}
