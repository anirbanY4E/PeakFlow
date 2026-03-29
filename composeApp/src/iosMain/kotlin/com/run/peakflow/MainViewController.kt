package com.run.peakflow

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.run.peakflow.di.appModule
import com.run.peakflow.presentation.components.RootComponent
import com.run.peakflow.data.network.SupabaseConfig
import com.run.peakflow.data.network.handleSupabaseDeepLink
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    val lifecycle = LifecycleRegistry()
    val componentContext = DefaultComponentContext(lifecycle = lifecycle)
    val rootComponent = RootComponent(componentContext)

    App(rootComponent)
}

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}

fun handleDeepLink(urlStr: String) {
    try {
        handleSupabaseDeepLink(SupabaseConfig.client, urlStr)
    } catch (e: Exception) {
        println("Error in handleDeepLink: ${e.message}")
    }
}