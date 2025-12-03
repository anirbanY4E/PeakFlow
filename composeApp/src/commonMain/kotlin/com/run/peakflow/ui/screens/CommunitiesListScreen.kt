package com.run.peakflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.presentation.components.CommunitiesListComponent
import com.run.peakflow.presentation.state.CommunitiesTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesListScreen(
    component: CommunitiesListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Communities",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        )

        // Tabs
        TabRow(
            selectedTabIndex = if (state.selectedTab == CommunitiesTab.MY_GROUPS) 0 else 1
        ) {
            Tab(
                selected = state.selectedTab == CommunitiesTab.MY_GROUPS,
                onClick = { component.onTabSelected(CommunitiesTab.MY_GROUPS) },
                text = { Text("My Groups") }
            )
            Tab(
                selected = state.selectedTab == CommunitiesTab.DISCOVER,
                onClick = { component.onTabSelected(CommunitiesTab.DISCOVER) },
                text = { Text("Discover") }
            )
        }

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { component.onRefresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val communities = if (state.selectedTab == CommunitiesTab.MY_GROUPS) {
                        state.myGroups
                    } else {
                        state.discoverGroups
                    }

                    if (communities.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (state.selectedTab == CommunitiesTab.MY_GROUPS) {
                                        "You haven't joined any communities yet"
                                    } else {
                                        "No communities to discover"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(8.dp)) }

                            items(communities, key = { it.id }) { community ->
                                CommunityCard(
                                    community = community,
                                    isMember = state.selectedTab == CommunitiesTab.MY_GROUPS,
                                    isPending = community.id in state.pendingRequestCommunityIds,
                                    onCommunityClick = { component.onCommunityClick(community.id) },
                                    onRequestToJoinClick = { component.onRequestToJoinClick(community.id) }
                                )
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityCard(
    community: CommunityGroup,
    isMember: Boolean,
    isPending: Boolean,
    onCommunityClick: () -> Unit,
    onRequestToJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCommunityClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // Community icon
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = community.category.emoji,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = community.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = community.city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Members
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${community.memberCount} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = community.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!isMember) {
                Spacer(modifier = Modifier.height(12.dp))

                if (isPending) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Request Pending")
                    }
                } else {
                    Button(
                        onClick = onRequestToJoinClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Request to Join")
                    }
                }
            }
        }
    }
}