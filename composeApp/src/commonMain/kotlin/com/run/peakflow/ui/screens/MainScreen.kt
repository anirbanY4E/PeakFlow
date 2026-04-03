package com.run.peakflow.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = state.selectedTab == item.tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { component.onTabSelected(item.tab) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = { 
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = state.selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
            },
            label = "tab_transition"
        ) { tab ->
            when (tab) {
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
}
