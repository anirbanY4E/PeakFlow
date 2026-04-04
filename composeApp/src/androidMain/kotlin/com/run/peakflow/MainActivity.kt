package com.run.peakflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import com.arkivanov.decompose.retainedComponent
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.permission.permissionUtil
import com.run.peakflow.presentation.components.RootComponent
import com.run.peakflow.presentation.navigation.DeepLinkNavigator

class MainActivity : ComponentActivity() {

    private val permissionUtil by permissionUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotifierManager.onCreateOrOnNewIntent(intent)
        permissionUtil.askNotificationPermission()
        com.run.peakflow.utils.ActivityProvider.currentActivity = this

        // Handle push notification deep link from cold start
        handlePushDeepLink(intent)

        // Use retainedComponent to keep the root component alive across configuration changes
        val rootComponent = retainedComponent { componentContext ->
            RootComponent(componentContext)
        }

        enableEdgeToEdge()

        setContent {
            App(rootComponent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
        // Handle push notification deep link from warm/background start
        handlePushDeepLink(intent)
    }

    private fun handlePushDeepLink(intent: Intent?) {
        val communityId = intent?.getStringExtra("community_id") ?: return
        val postId = intent.getStringExtra("post_id")
        DeepLinkNavigator.navigate(
            DeepLinkNavigator.DeepLink.CommunityPost(
                communityId = communityId,
                postId = postId
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (com.run.peakflow.utils.ActivityProvider.currentActivity == this) {
            com.run.peakflow.utils.ActivityProvider.currentActivity = null
        }
    }
}

