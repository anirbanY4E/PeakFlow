package com.run.peakflow

import android.app.Application
import com.run.peakflow.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PeakFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PeakFlowApp)
            modules(appModule)
        }
    }
}
