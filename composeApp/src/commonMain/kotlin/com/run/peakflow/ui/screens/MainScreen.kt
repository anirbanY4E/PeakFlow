package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.MainComponent
import com.run.peakflow.presentation.state.MainTab

data class BottomNavItem(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(
        tab = MainTab.FEED,
        label = "Feed",
        selectedIcon = Icons.Filled.DynamicFeed,
        unselectedIcon = Icons.Outlined.DynamicFeed
    ),
    BottomNavItem(
        tab = MainTab.EVENTS,
        label = "Events",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    BottomNavItem(
        tab = MainTab.COMMUNITIES,
        label = "Groups",
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups
    ),
    BottomNavItem(
        tab = MainTab.PROFILE,
        label = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

@Composable
fun MainScreen(component: MainComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                bottomNavItems.forEach { item ->
                    val selected = state.selectedTab == item.tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { component.onTabSelected(item.tab) },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { 
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        when (state.selectedTab) {
            MainTab.FEED -> FeedScreen(
                component = component.feedComponent,
                modifier = Modifier.padding(padding)
            )
            MainTab.EVENTS -> EventsListScreen(
                component = component.eventsListComponent,
                modifier = Modifier.padding(padding)
            )
            MainTab.COMMUNITIES -> CommunitiesListScreen(
                component = component.communitiesListComponent,
                modifier = Modifier.padding(padding)
            )
            MainTab.PROFILE -> ProfileScreen(
                component = component.profileComponent,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
