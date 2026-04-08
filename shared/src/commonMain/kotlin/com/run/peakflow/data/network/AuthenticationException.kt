package com.run.peakflow.data.network

/**
 * Thrown when an operation requires an authenticated user, but the session is missing,
 * expired, or could not be refreshed.
 */
class AuthenticationException(message: String = "User is not authenticated") : Exception(message)
