package com.run.peakflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.SettingsComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Notifications section
            SectionHeader(title = "Notifications")

            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Push Notifications",
                subtitle = "Receive push notifications",
                isChecked = state.pushNotificationsEnabled,
                onToggle = { component.onPushNotificationsToggle() }
            )

            SettingsToggleItem(
                icon = Icons.Default.Email,
                title = "Email Updates",
                subtitle = "Weekly digest and event reminders",
                isChecked = state.emailUpdatesEnabled,
                onToggle = { component.onEmailUpdatesToggle() }
            )

            SettingsToggleItem(
                icon = Icons.Default.Phone,
                title = "WhatsApp Reminders",
                subtitle = "Event reminders via WhatsApp",
                isChecked = state.whatsappRemindersEnabled,
                onToggle = { component.onWhatsappRemindersToggle() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Appearance section
            SectionHeader(title = "Appearance")

            SettingsToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Dark Mode",
                subtitle = "Use dark theme",
                isChecked = state.darkModeEnabled,
                onToggle = { component.onDarkModeToggle() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Account section
            SectionHeader(title = "Account")

            SettingsClickItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                subtitle = "Sign out of your account",
                onClick = { component.onLogoutClick() },
                isDestructive = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App version
            Text(
                text = "PeakFlow v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // Logout confirmation dialog
        if (state.isLogoutConfirmVisible) {
            AlertDialog(
                onDismissRequest = { component.onLogoutCancel() },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    TextButton(onClick = { component.onLogoutConfirm() }) {
                        Text(
                            text = "Sign Out",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { component.onLogoutCancel() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}