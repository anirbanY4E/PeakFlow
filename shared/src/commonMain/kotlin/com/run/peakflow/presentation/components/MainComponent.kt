package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.run.peakflow.presentation.state.MainState
import com.run.peakflow.presentation.state.MainTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainComponent(
    componentContext: ComponentContext,
    val onNavigateToCommunityDetail: (String) -> Unit,
    val onNavigateToEventDetail: (String) -> Unit,
    val onNavigateToPostDetail: (String) -> Unit,
    val onNavigateToSettings: () -> Unit,
    val onLogout: () -> Unit
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val backCallback = BackCallback(isEnabled = false) {
        // Go back to Feed tab instead of exiting
        onTabSelected(MainTab.FEED)
    }

    init {
        backHandler.register(backCallback)
        updateBackCallback()
    }

    val feedComponent = FeedComponent(
        componentContext = componentContext,
        onNavigateToPostDetail = onNavigateToPostDetail,
        onNavigateToCommunityDetail = onNavigateToCommunityDetail
    )

    val eventsListComponent = EventsListComponent(
        componentContext = componentContext,
        onNavigateToEventDetail = onNavigateToEventDetail
    )

    val communitiesListComponent = CommunitiesListComponent(
        componentContext = componentContext,
        onNavigateToCommunityDetail = onNavigateToCommunityDetail
    )

    val profileComponent = ProfileComponent(
        componentContext = componentContext,
        onNavigateToSettings = onNavigateToSettings,
        onLogout = onLogout
    )

    fun onTabSelected(tab: MainTab) {
        _state.update { it.copy(selectedTab = tab) }
        updateBackCallback()
    }

    private fun updateBackCallback() {
        // Enable back callback only when NOT on Feed tab
        backCallback.isEnabled = _state.value.selectedTab != MainTab.FEED
    }
}