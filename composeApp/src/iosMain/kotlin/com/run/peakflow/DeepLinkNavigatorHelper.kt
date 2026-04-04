package com.run.peakflow

import com.run.peakflow.presentation.navigation.DeepLinkNavigator

/**
 * Helper class exposed to Swift (via the ComposeApp framework)
 * that bridges iOS notification tap events to the shared DeepLinkNavigator.
 */
object DeepLinkNavigatorHelper {
    fun navigateToCommunityPost(communityId: String, postId: String?) {
        DeepLinkNavigator.navigate(
            DeepLinkNavigator.DeepLink.CommunityPost(
                communityId = communityId,
                postId = postId
            )
        )
    }
}
