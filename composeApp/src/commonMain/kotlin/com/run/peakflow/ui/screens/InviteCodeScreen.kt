package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.presentation.components.InviteCodeComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteCodeScreen(component: InviteCodeComponent) {
    val state by component.state.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PeakFlowSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap * 2))

            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Text(
                text = "Join a Community",
                style = PeakFlowTypography.screenTitle()
            )

            Text(
                text = "Enter the code shared with you to join your local fitness tribe",
                style = PeakFlowTypography.bodyMain(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.code,
                onValueChange = { component.onCodeChanged(it.uppercase()) },
                label = { Text("Invite Code") },
                placeholder = { Text("e.g. CUBBON-1234") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                textStyle = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Button(
                onClick = { component.onJoinClick() },
                enabled = !state.isLoading && state.code.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("JOIN COMMUNITY")
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { /* Handle no code */ }) {
                Text("I don't have a code", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
        }
    }
}
