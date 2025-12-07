package com.run.peakflow.di

import com.run.peakflow.data.auth.GoogleAuthProvider
import com.run.peakflow.data.auth.JvmGoogleAuthProvider

actual fun platformGoogleAuthProvider(): GoogleAuthProvider {
    return JvmGoogleAuthProvider()
}
