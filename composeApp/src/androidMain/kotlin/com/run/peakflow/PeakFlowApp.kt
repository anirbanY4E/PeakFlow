package com.run.peakflow

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.run.peakflow.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PeakFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = android.R.drawable.ic_dialog_info,
                showPushNotification = true,
            )
        )
        startKoin {
            androidContext(this@PeakFlowApp)
            modules(appModule)
        }
    }
}
