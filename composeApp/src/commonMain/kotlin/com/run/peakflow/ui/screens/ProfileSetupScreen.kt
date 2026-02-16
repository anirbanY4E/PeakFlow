package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.presentation.components.ProfileSetupComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(component: ProfileSetupComponent) {
    val state by component.state.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PeakFlowSpacing.screenHorizontal)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap * 2))

            Text(
                text = "Complete Profile",
                style = PeakFlowTypography.screenTitle(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Help us personalize your experience",
                style = PeakFlowTypography.bodyMain(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = { component.onNameChanged(it) },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

            OutlinedTextField(
                value = state.city,
                onValueChange = { component.onCityChanged(it) },
                label = { Text("City") },
                leadingIcon = { Icon(Icons.Default.LocationCity, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Text(
                text = "Your Interests",
                style = PeakFlowTypography.sectionHeader(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category in state.selectedInterests,
                        onClick = { component.onInterestToggled(category) },
                        label = { Text("${category.emoji} ${category.displayName}") }
                    )
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Button(
                onClick = { component.onCompleteClick() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("FINISH SETUP")
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
        }
    }
}
