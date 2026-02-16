package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.SettingsComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = PeakFlowTypography.bodyTitle()) },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Notifications",
                style = PeakFlowTypography.sectionHeader(),
                modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
            )

            ListItem(
                headlineContent = { Text("Push Notifications", style = PeakFlowTypography.bodyMain()) },
                leadingContent = { Icon(Icons.Default.Notifications, null) },
                trailingContent = { 
                    Switch(
                        checked = state.pushNotificationsEnabled, 
                        onCheckedChange = { component.onPushNotificationsToggle() }
                    ) 
                }
            )

            ListItem(
                headlineContent = { Text("Email Updates", style = PeakFlowTypography.bodyMain()) },
                leadingContent = { Icon(Icons.Default.Email, null) },
                trailingContent = { 
                    Switch(
                        checked = state.emailUpdatesEnabled, 
                        onCheckedChange = { component.onEmailUpdatesToggle() }
                    ) 
                }
            )

            ListItem(
                headlineContent = { Text("WhatsApp Reminders", style = PeakFlowTypography.bodyMain()) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Chat, null) },
                trailingContent = { 
                    Switch(
                        checked = state.whatsappRemindersEnabled, 
                        onCheckedChange = { component.onWhatsappRemindersToggle() }
                    ) 
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = PeakFlowSpacing.elementGap))

            Text(
                text = "Appearance",
                style = PeakFlowTypography.sectionHeader(),
                modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
            )

            ListItem(
                headlineContent = { Text("Dark Mode", style = PeakFlowTypography.bodyMain()) },
                leadingContent = { Icon(Icons.Default.Brightness4, null) },
                trailingContent = { 
                    Switch(
                        checked = state.darkModeEnabled, 
                        onCheckedChange = { component.onDarkModeToggle() }
                    ) 
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { component.onLogoutClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PeakFlowSpacing.screenHorizontal)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOGOUT")
            }
            
            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
        }
    }

    if (state.isLogoutConfirmVisible) {
        AlertDialog(
            onDismissRequest = { component.onLogoutCancel() },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to sign out of PeakFlow?") },
            confirmButton = {
                TextButton(onClick = { component.onLogoutConfirm() }) {
                    Text("LOGOUT", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { component.onLogoutCancel() }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
