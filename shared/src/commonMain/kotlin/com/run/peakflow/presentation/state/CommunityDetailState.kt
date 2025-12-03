package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.models.Post

data class CommunityDetailState(
    val community: CommunityGroup? = null,
    val posts: List<Post> = emptyList(),
    val events: List<Event> = emptyList(),
    val members: List<CommunityMembership> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTab: CommunityTab = CommunityTab.POSTS,
    val userRole: MembershipRole? = null,
    val likedPostIds: Set<String> = emptySet(),
    val rsvpedEventIds: Set<String> = emptySet()
)

enum class CommunityTab {
    POSTS,
    EVENTS,
    MEMBERS,
    ABOUT
}