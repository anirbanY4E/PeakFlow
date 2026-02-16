package com.run.peakflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.presentation.components.EventsListComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsListScreen(
    component: EventsListComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Explore Events",
            style = PeakFlowTypography.screenTitle(),
            modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PeakFlowSpacing.screenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedCategory == null,
                onClick = { component.onCategorySelected(null) },
                label = { Text("All", style = MaterialTheme.typography.labelMedium) }
            )
            EventCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.selectedCategory == category,
                    onClick = { component.onCategorySelected(category) },
                    label = { Text("${category.emoji} ${category.displayName}", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { component.onRefresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = PeakFlowSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(PeakFlowSpacing.elementGap)
            ) {
                items(state.events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        isRsvped = event.id in state.rsvpedEventIds,
                        onEventClick = { component.onEventClick(event.id) },
                        onRsvpClick = { component.onRsvpClick(event.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap)) }
            }
        }
    }
}

@Composable
fun EventCard(event: Event, isRsvped: Boolean, onEventClick: () -> Unit, onRsvpClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEventClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(PeakFlowSpacing.cardPadding)) {
            Text(event.category.displayName.uppercase(), style = PeakFlowTypography.labelSecondary(), color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(event.title, style = PeakFlowTypography.bodyTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            
            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${event.date} • ${event.time}", style = PeakFlowTypography.labelSecondary())
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(event.location, style = PeakFlowTypography.labelSecondary(), maxLines = 1)
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.cardPadding))
            Button(
                onClick = onRsvpClick,
                enabled = !isRsvped && event.currentParticipants < event.maxParticipants,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isRsvped) "RSVP'd" else "Reserve Spot", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
