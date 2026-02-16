package com.run.peakflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.presentation.components.CommunitiesListComponent
import com.run.peakflow.presentation.state.CommunitiesTab
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesListScreen(
    component: CommunitiesListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Communities",
            style = PeakFlowTypography.screenTitle(),
            modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
        )

        TabRow(
            selectedTabIndex = if (state.selectedTab == CommunitiesTab.MY_GROUPS) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            divider = { HorizontalDivider(thickness = 0.5.dp) }
        ) {
            Tab(
                selected = state.selectedTab == CommunitiesTab.MY_GROUPS,
                onClick = { component.onTabSelected(CommunitiesTab.MY_GROUPS) },
                text = { Text("MY GROUPS", style = MaterialTheme.typography.labelMedium) }
            )
            Tab(
                selected = state.selectedTab == CommunitiesTab.DISCOVER,
                onClick = { component.onTabSelected(CommunitiesTab.DISCOVER) },
                text = { Text("DISCOVER", style = MaterialTheme.typography.labelMedium) }
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { component.onRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            val communities = if (state.selectedTab == CommunitiesTab.MY_GROUPS) state.myGroups else state.discoverGroups

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = PeakFlowSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(PeakFlowSpacing.elementGap)
            ) {
                item { Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap)) }
                items(communities, key = { it.id }) { community ->
                    CommunityCard(
                        community = community,
                        isMember = state.selectedTab == CommunitiesTab.MY_GROUPS,
                        isPending = community.id in state.pendingRequestCommunityIds,
                        onCommunityClick = { component.onCommunityClick(community.id) },
                        onRequestToJoinClick = { component.onRequestToJoinClick(community.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityCard(community: CommunityGroup, isMember: Boolean, isPending: Boolean, onCommunityClick: () -> Unit, onRequestToJoinClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCommunityClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.padding(PeakFlowSpacing.cardPadding), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = community.category.emoji, style = MaterialTheme.typography.headlineSmall)
                }
            }
            Spacer(modifier = Modifier.width(PeakFlowSpacing.elementGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(community.title, style = PeakFlowTypography.bodyTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${community.memberCount} members • ${community.city}", style = PeakFlowTypography.labelSecondary())
            }
            if (!isMember) {
                IconButton(onClick = onRequestToJoinClick, enabled = !isPending) {
                    Icon(
                        imageVector = if (isPending) Icons.Default.HourglassBottom else Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
