package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.CommunityGroup

data class CommunitiesListState(
    val myGroups: List<CommunityGroup> = emptyList(),
    val discoverGroups: List<CommunityGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTab: CommunitiesTab = CommunitiesTab.MY_GROUPS,
    val searchQuery: String = "",
    val pendingRequestCommunityIds: Set<String> = emptySet()
)

enum class CommunitiesTab {
    MY_GROUPS,
    DISCOVER
}