package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.presentation.components.EventDetailComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(component: EventDetailComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details", style = PeakFlowTypography.bodyTitle()) },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.event != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PeakFlowSpacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

                // Category Tag
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${state.event!!.category.emoji} ${state.event!!.category.displayName}".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

                Text(
                    text = state.event!!.title,
                    style = PeakFlowTypography.screenTitle()
                )

                if (state.community != null) {
                    Text(
                        text = "by ${state.community!!.title}",
                        style = PeakFlowTypography.labelSecondary(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

                // Logistics Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(PeakFlowSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(PeakFlowSpacing.elementGap)
                    ) {
                        DetailRow(Icons.Default.CalendarToday, "Date", state.event!!.date)
                        DetailRow(Icons.Default.Schedule, "Time", state.event!!.time)
                        DetailRow(Icons.Default.LocationOn, "Location", state.event!!.location)
                    }
                }

                Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

                Text("About", style = PeakFlowTypography.sectionHeader())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.event!!.description,
                    style = PeakFlowTypography.bodyMain(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

                // Capacity
                Text("Availability", style = PeakFlowTypography.sectionHeader())
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.event!!.currentParticipants.toFloat() / state.event!!.maxParticipants.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${state.event!!.currentParticipants} going", style = PeakFlowTypography.labelSecondary())
                    Text("${state.event!!.maxParticipants} max", style = PeakFlowTypography.labelSecondary())
                }

                Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap * 2))

                // Action
                EventActionButtons(state, component)

                Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
            }
        }
    }
}

@Composable
private fun EventActionButtons(state: com.run.peakflow.presentation.state.EventDetailState, component: EventDetailComponent) {
    if (state.hasRsvped) {
        Column {
            Button(
                onClick = { if (!state.hasCheckedIn) component.onCheckInClick() },
                enabled = !state.hasCheckedIn && !state.isCheckInLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.hasCheckedIn) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CHECKED IN")
                } else {
                    Text("CHECK IN NOW")
                }
            }
            if (!state.hasCheckedIn) {
                TextButton(
                    onClick = { component.onCancelRsvpClick() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isRsvpLoading
                ) {
                    Text("Cancel RSVP", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    } else {
        Button(
            onClick = { component.onRsvpClick() },
            enabled = !state.isRsvpLoading && state.event!!.currentParticipants < state.event!!.maxParticipants,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("RESERVE SPOT")
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = PeakFlowTypography.labelSecondary(), fontSize = 11.sp)
            Text(value, style = PeakFlowTypography.bodyMain(), fontWeight = FontWeight.Medium)
        }
    }
}
