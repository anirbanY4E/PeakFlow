package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.presentation.components.GenerateInviteComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateInviteScreen(component: GenerateInviteComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Members", style = PeakFlowTypography.bodyTitle()) },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = PeakFlowSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(PeakFlowSpacing.sectionGap)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (state.generatedCode != null) {
                item {
                    GeneratedCodeCard(
                        code = state.generatedCode!!,
                        isCopied = state.isCopied,
                        onCopyClick = { component.onCopyCode() }
                    )
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(PeakFlowSpacing.cardPadding)) {
                        Text("Generate New Code", style = PeakFlowTypography.sectionHeader())
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Expires in ${state.expiresInDays} days", style = PeakFlowTypography.bodyMain())
                        Slider(
                            value = state.expiresInDays.toFloat(),
                            onValueChange = { component.onExpiryDaysChanged(it.toInt()) },
                            valueRange = 1f..30f,
                            steps = 28
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { component.onGenerateClick() },
                            enabled = !state.isGenerating,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (state.isGenerating) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Text("GENERATE")
                        }
                    }
                }
            }

            if (state.existingCodes.isNotEmpty()) {
                item {
                    Text("ACTIVE CODES", style = PeakFlowTypography.sectionHeader())
                }
                items(state.existingCodes, key = { it.id }) { invite ->
                    ExistingCodeCard(invite = invite)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GeneratedCodeCard(code: InviteCode, isCopied: Boolean, onCopyClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Invite Code", style = PeakFlowTypography.labelSecondary())
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = code.code,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCopyClick, shape = RoundedCornerShape(8.dp)) {
                    Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCopied) "COPIED" else "COPY")
                }
                OutlinedButton(onClick = {}, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Text("SHARE", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExistingCodeCard(invite: InviteCode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        ListItem(
            headlineContent = { Text(invite.code, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace)) },
            supportingContent = { Text("Used ${invite.currentUses} times", style = PeakFlowTypography.labelSecondary()) },
            trailingContent = {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (invite.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        if (invite.isActive) "Active" else "Expired",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        )
    }
}
