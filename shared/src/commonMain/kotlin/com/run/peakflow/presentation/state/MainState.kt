package com.run.peakflow.presentation.state

data class MainState(
    val selectedTab: MainTab = MainTab.FEED,
    val unreadNotifications: Int = 0
)

enum class MainTab {
    FEED,
    EVENTS,
    COMMUNITIES,
    PROFILE
}