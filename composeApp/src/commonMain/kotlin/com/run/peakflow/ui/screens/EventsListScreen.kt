package com.run.peakflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.presentation.components.EventsListComponent
import com.run.peakflow.ui.components.EventCardSkeleton
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsListScreen(
    component: EventsListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            text = "Explore Events",
            style = PeakFlowTypography.screenTitle(),
            modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = PeakFlowSpacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = state.selectedCategory == null,
                onClick = { component.onCategorySelected(null) },
                label = { Text("All", style = MaterialTheme.typography.labelLarge) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            )
            EventCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.selectedCategory == category,
                    onClick = { component.onCategorySelected(category) },
                    label = {
                        Text(
                            text = "${category.emoji} ${category.displayName}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val showSkeletons = state.isLoading && state.events.isEmpty()

        PullToRefreshBox(
            isRefreshing = state.isRefreshing && !showSkeletons,
            onRefresh = { component.onRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (showSkeletons) {
                // Show skeleton loaders for initial load
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = PeakFlowSpacing.screenHorizontal,
                        end = PeakFlowSpacing.screenHorizontal,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) {
                        EventCardSkeleton()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = PeakFlowSpacing.screenHorizontal,
                        end = PeakFlowSpacing.screenHorizontal,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.events, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            isRsvped = event.id in state.rsvpedEventIds,
                            onEventClick = { component.onEventClick(event.id) },
                            onRsvpClick = { component.onRsvpClick(event.id) },
                            isRsvping = event.id in state.rsvpingEventIds
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(event: Event, isRsvped: Boolean, onEventClick: () -> Unit, onRsvpClick: () -> Unit, canRsvp: Boolean = true, isRsvping: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onEventClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                ) {
                    Text(
                        text = event.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Groups,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${event.currentParticipants}/${event.maxParticipants}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = event.title,
                style = PeakFlowTypography.bodyTitle().copy(fontSize = 18.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${event.date} • ${event.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            val isFull = event.currentParticipants >= event.maxParticipants && !isRsvped
            
            Button(
                onClick = onRsvpClick,
                enabled = !isRsvped && !isFull && canRsvp && !isRsvping,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRsvped) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    disabledContainerColor = if (isRsvping) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else if (!canRsvp) MaterialTheme.colorScheme.surfaceVariant else if (isFull) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                if (isRsvping) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            !canRsvp -> "Members Only"
                            isRsvped -> "RSVP'd ✓"
                            isFull -> "Event Full"
                            else -> "Reserve Spot"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (!canRsvp) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                    )
                }
            }
        }
    }
}