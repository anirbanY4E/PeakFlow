package com.run.peakflow

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.run.peakflow.presentation.components.RootComponent
import com.run.peakflow.ui.screens.CommunityDetailScreen
import com.run.peakflow.ui.screens.EventDetailScreen
import com.run.peakflow.ui.screens.HomeScreen
import com.run.peakflow.ui.screens.OnboardingScreen
import com.run.peakflow.ui.theme.PeakFlowTheme

@Composable
fun App(rootComponent: RootComponent) {
    PeakFlowTheme {
        Children(
            stack = rootComponent.childStack,
            animation = stackAnimation(fade())
        ) { child ->
            when (val instance = child.instance) {
                is RootComponent.Child.Onboarding -> {
                    OnboardingScreen(component = instance.component)
                }
                is RootComponent.Child.Home -> {
                    HomeScreen(component = instance.component)
                }
                is RootComponent.Child.CommunityDetail -> {
                    CommunityDetailScreen(component = instance.component)
                }
                is RootComponent.Child.EventDetail -> {
                    EventDetailScreen(component = instance.component)
                }
            }
        }
    }
}