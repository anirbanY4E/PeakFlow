package com.run.peakflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PeakFlowSpacing {
    val screenHorizontal = 20.dp
    val sectionGap = 24.dp
    val elementGap = 12.dp
    val cardPadding = 16.dp
    val topBarHeight = 56.dp
}

object PeakFlowTypography {
    @Composable
    fun screenTitle() = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    @Composable
    fun sectionHeader() = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )

    @Composable
    fun bodyTitle() = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.SemiBold
    )

    @Composable
    fun bodyMain() = MaterialTheme.typography.bodyLarge

    @Composable
    fun labelSecondary() = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
