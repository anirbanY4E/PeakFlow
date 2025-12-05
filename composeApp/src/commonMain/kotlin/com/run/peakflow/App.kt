package com.run.peakflow

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.run.peakflow.presentation.components.RootComponent
import com.run.peakflow.ui.screens.CommunityDetailScreen
import com.run.peakflow.ui.screens.CreateEventScreen
import com.run.peakflow.ui.screens.CreatePostScreen
import com.run.peakflow.ui.screens.EventDetailScreen
import com.run.peakflow.ui.screens.GenerateInviteScreen
import com.run.peakflow.ui.screens.InviteCodeScreen
import com.run.peakflow.ui.screens.JoinRequestsScreen
import com.run.peakflow.ui.screens.MainScreen
import com.run.peakflow.ui.screens.OtpVerificationScreen
import com.run.peakflow.ui.screens.PostDetailScreen
import com.run.peakflow.ui.screens.ProfileSetupScreen
import com.run.peakflow.ui.screens.SettingsScreen
import com.run.peakflow.ui.screens.SignInScreen
import com.run.peakflow.ui.screens.SignUpScreen
import com.run.peakflow.ui.screens.SplashScreen
import com.run.peakflow.ui.screens.WelcomeScreen
import com.run.peakflow.ui.theme.PeakFlowTheme

@Composable
fun App(rootComponent: RootComponent) {
    PeakFlowTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val childStack by rootComponent.childStack.subscribeAsState()

            Children(
                stack = childStack,
                animation = stackAnimation(fade() + scale())
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Splash -> SplashScreen(instance.component)
                    is RootComponent.Child.Welcome -> WelcomeScreen(instance.component)
                    is RootComponent.Child.SignUp -> SignUpScreen(instance.component)
                    is RootComponent.Child.SignIn -> SignInScreen(instance.component)
                    is RootComponent.Child.OtpVerification -> OtpVerificationScreen(instance.component)
                    is RootComponent.Child.InviteCode -> InviteCodeScreen(instance.component)
                    is RootComponent.Child.ProfileSetup -> ProfileSetupScreen(instance.component)
                    is RootComponent.Child.Main -> MainScreen(instance.component)
                    is RootComponent.Child.CommunityDetail -> CommunityDetailScreen(instance.component)
                    is RootComponent.Child.EventDetail -> EventDetailScreen(instance.component)
                    is RootComponent.Child.PostDetail -> PostDetailScreen(instance.component)
                    is RootComponent.Child.GenerateInvite -> GenerateInviteScreen(instance.component)
                    is RootComponent.Child.JoinRequests -> JoinRequestsScreen(instance.component)
                    is RootComponent.Child.Settings -> SettingsScreen(instance.component)
                    is RootComponent.Child.CreateEvent -> CreateEventScreen(instance.component)
                    is RootComponent.Child.CreatePost -> CreatePostScreen(instance.component)
                }
            }
        }
    }
}