package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.run.peakflow.presentation.components.ProfileComponent
import com.run.peakflow.presentation.state.ProfileStats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    component: ProfileComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { component.onSettingsClick() }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Settings, "Settings", modifier = Modifier.size(20.dp))
            }
        }

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            state.user != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(state.user!!.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(state.user!!.city, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(24.dp))
                    StatsRow(stats = state.stats)

                    if (state.interests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Your Interests", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.interests.forEach { category ->
                                    SuggestionChip(onClick = {}, label = { Text("${category.emoji} ${category.displayName}") })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: ProfileStats) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatItem(icon = Icons.Default.Groups, value = stats.communitiesCount.toString(), label = "Groups")
            StatItem(icon = Icons.Default.Event, value = stats.eventsAttended.toString(), label = "Events")
            StatItem(icon = Icons.Default.EmojiEvents, value = stats.points.toString(), label = "Points")
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
