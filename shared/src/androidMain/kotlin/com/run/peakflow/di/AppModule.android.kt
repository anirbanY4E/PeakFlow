package com.run.peakflow.di

import com.run.peakflow.data.auth.AndroidGoogleAuthProvider
import com.run.peakflow.data.auth.GoogleAuthProvider

actual fun platformGoogleAuthProvider(): GoogleAuthProvider {
    return AndroidGoogleAuthProvider()
}
