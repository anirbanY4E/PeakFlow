package com.run.peakflow.presentation.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A shared singleton that bridges push notification tap events
 * to the Decompose navigation stack.
 *
 * NotifierManager.Listener posts events here;
 * RootComponent collects them and navigates.
 */
object DeepLinkNavigator {

    sealed class DeepLink {
        data class CommunityPost(
            val communityId: String,
            val postId: String? = null
        ) : DeepLink()
    }

    private val _events = MutableSharedFlow<DeepLink>(extraBufferCapacity = 1)
    val events: SharedFlow<DeepLink> = _events.asSharedFlow()

    fun navigate(deepLink: DeepLink) {
        _events.tryEmit(deepLink)
    }
}
