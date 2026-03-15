package com.run.peakflow.utils

import android.app.Activity
import java.lang.ref.WeakReference

/**
 * Provides access to the current foreground Activity for operations
 * that require an Activity Context (e.g., Google Sign-In with CredentialManager).
 */
object ActivityProvider {
    private var activityRef: WeakReference<Activity>? = null

    var currentActivity: Activity?
        get() = activityRef?.get()
        set(value) {
            activityRef = if (value != null) WeakReference(value) else null
        }
}
