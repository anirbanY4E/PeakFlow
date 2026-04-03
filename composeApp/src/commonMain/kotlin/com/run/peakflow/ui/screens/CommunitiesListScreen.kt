package com.run.peakflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.presentation.components.CommunitiesListComponent
import com.run.peakflow.presentation.state.CommunitiesTab
import com.run.peakflow.ui.components.CommunityImage
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitiesListScreen(
    component: CommunitiesListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            text = "Communities",
            style = PeakFlowTypography.screenTitle(),
            modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
        )

        // Modern Tab Bar using standard ScrollableTabRow for compatibility
        val selectedIndex = if (state.selectedTab == CommunitiesTab.MY_GROUPS) 0 else 1
        
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            divider = {},
            edgePadding = 20.dp,
            indicator = { tabPositions ->
                if (selectedIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            Tab(
                selected = state.selectedTab == CommunitiesTab.MY_GROUPS,
                onClick = { component.onTabSelected(CommunitiesTab.MY_GROUPS) },
                text = { 
                    Text(
                        "MY GROUPS", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (state.selectedTab == CommunitiesTab.MY_GROUPS) FontWeight.Bold else FontWeight.Medium
                    ) 
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = state.selectedTab == CommunitiesTab.DISCOVER,
                onClick = { component.onTabSelected(CommunitiesTab.DISCOVER) },
                text = { 
                    Text(
                        "DISCOVER", 
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (state.selectedTab == CommunitiesTab.DISCOVER) FontWeight.Bold else FontWeight.Medium
                    ) 
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { component.onRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            val communities = if (state.selectedTab == CommunitiesTab.MY_GROUPS) state.myGroups else state.discoverGroups

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = PeakFlowSpacing.screenHorizontal,
                    end = PeakFlowSpacing.screenHorizontal,
                    top = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCommunityClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            CommunityImage(
                imageUrl = community.imageUrl,
                emoji = community.category.emoji,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentDescription = "${community.title} image"
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = community.title, 
                    style = PeakFlowTypography.bodyTitle().copy(fontSize = 16.sp), 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Groups, 
                        null, 
                        modifier = Modifier.size(14.dp), 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${community.memberCount} members", 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•", 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = community.city, 
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!isMember) {
                Surface(
                    onClick = onRequestToJoinClick,
                    enabled = !isPending,
                    shape = CircleShape,
                    color = if (isPending) MaterialTheme.colorScheme.surfaceVariant 
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPending) Icons.Default.HourglassBottom else Icons.Default.Add,
                            contentDescription = "Join",
                            modifier = Modifier.size(20.dp),
                            tint = if (isPending) MaterialTheme.colorScheme.onSurfaceVariant 
                                   else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp), 
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
