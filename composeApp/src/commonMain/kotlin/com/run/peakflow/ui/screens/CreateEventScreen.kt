package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.presentation.components.CreateEventComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(component: CreateEventComponent) {
    val state by component.state.collectAsState()
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Event", style = PeakFlowTypography.bodyTitle()) },
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
                .padding(horizontal = PeakFlowSpacing.screenHorizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PeakFlowSpacing.elementGap)
        ) {
            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

            Text("Event Details", style = PeakFlowTypography.sectionHeader())

            OutlinedTextField(
                value = state.title,
                onValueChange = { component.onTitleChanged(it) },
                label = { Text("Event Title") },
                placeholder = { Text("e.g. Morning 5K Run") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = "${state.category.emoji} ${state.category.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    EventCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text("${category.emoji} ${category.displayName}") },
                            onClick = { component.onCategoryChanged(category); categoryExpanded = false }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PeakFlowSpacing.elementGap)) {
                OutlinedTextField(
                    value = state.date,
                    onValueChange = { component.onDateChanged(it) },
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = state.time,
                    onValueChange = { component.onTimeChanged(it) },
                    label = { Text("Time") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                )
            }

            OutlinedTextField(
                value = state.location,
                onValueChange = { component.onLocationChanged(it) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { component.onDescriptionChanged(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Attendance & Pricing", style = PeakFlowTypography.sectionHeader())

            OutlinedTextField(
                value = state.maxParticipants.toString(),
                onValueChange = { it.toIntOrNull()?.let { max -> component.onMaxParticipantsChanged(max) } },
                label = { Text("Max Participants") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = state.isFree, onClick = { component.onIsFreeChanged(true) })
                    Text("Free Event", style = PeakFlowTypography.bodyMain())
                }
                Spacer(modifier = Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !state.isFree, onClick = { component.onIsFreeChanged(false) })
                    Text("Paid", style = PeakFlowTypography.bodyMain())
                }
            }

            if (!state.isFree) {
                OutlinedTextField(
                    value = state.price?.toString() ?: "",
                    onValueChange = { component.onPriceChanged(it.toDoubleOrNull()) },
                    label = { Text("Price (INR)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium
                )
            }

            if (state.error != null) {
                Text(text = state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { component.onCreateClick() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("CREATE EVENT")
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
        }
    }
}
