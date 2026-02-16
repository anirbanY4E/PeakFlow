package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.JoinRequestsComponent
import com.run.peakflow.presentation.state.JoinRequestWithUser
import com.run.peakflow.ui.components.EmptyView
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRequestsScreen(component: JoinRequestsComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Requests", style = PeakFlowTypography.bodyTitle()) },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.pendingRequests.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.pendingRequests.isEmpty() -> {
                EmptyView(
                    title = "All caught up!",
                    message = "No pending join requests at the moment.",
                    icon = { Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline) }
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { component.onRefresh() },
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = PeakFlowSpacing.screenHorizontal),
                        verticalArrangement = Arrangement.spacedBy(PeakFlowSpacing.elementGap)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        items(state.pendingRequests, key = { it.request.id }) { requestWithUser ->
                            JoinRequestCard(
                                requestWithUser = requestWithUser,
                                isProcessing = requestWithUser.request.id in state.processingRequestIds,
                                onApproveClick = { component.onApproveClick(requestWithUser.request.id) },
                                onRejectClick = { component.onRejectClick(requestWithUser.request.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinRequestCard(requestWithUser: JoinRequestWithUser, isProcessing: Boolean, onApproveClick: () -> Unit, onRejectClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        ListItem(
            headlineContent = { Text(requestWithUser.user?.name ?: "Unknown User", style = PeakFlowTypography.bodyTitle()) },
            supportingContent = { Text("Wants to join community", style = PeakFlowTypography.labelSecondary()) },
            leadingContent = {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp))
                }
            },
            trailingContent = {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Row {
                        IconButton(onClick = onRejectClick) {
                            Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = onApproveClick) {
                            Icon(Icons.Default.Check, "Approve", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        )
    }
}
