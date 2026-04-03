package com.run.peakflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Create Event", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "BASIC INFORMATION")

            OutlinedTextField(
                value = state.title,
                onValueChange = { component.onTitleChanged(it) },
                label = { Text("Event Title") },
                placeholder = { Text("e.g. Morning 5K Run") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
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
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded, 
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    EventCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text("${category.emoji} ${category.displayName}", style = MaterialTheme.typography.bodyLarge) },
                            onClick = { component.onCategoryChanged(category); categoryExpanded = false }
                        )
                    }
                }
            }

            SectionHeader(title = "DATE & LOCATION")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.date,
                    onValueChange = { component.onDateChanged(it) },
                    label = { Text("Date") },
                    placeholder = { Text("DD/MM/YYYY") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(18.dp)) }
                )
                OutlinedTextField(
                    value = state.time,
                    onValueChange = { component.onTimeChanged(it) },
                    label = { Text("Time") },
                    placeholder = { Text("06:00 AM") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp)) }
                )
            }

            OutlinedTextField(
                value = state.location,
                onValueChange = { component.onLocationChanged(it) },
                label = { Text("Location") },
                placeholder = { Text("Meetup point address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { component.onDescriptionChanged(it) },
                label = { Text("Description") },
                placeholder = { Text("What should participants expect?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(16.dp)
            )

            SectionHeader(title = "CAPACITY & PRICING")

            OutlinedTextField(
                value = if (state.maxParticipants == 0) "" else state.maxParticipants.toString(),
                onValueChange = { it.toIntOrNull()?.let { max -> component.onMaxParticipantsChanged(max) } },
                label = { Text("Max Participants") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Group, null, modifier = Modifier.size(18.dp)) }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.isFree, onClick = { component.onIsFreeChanged(true) })
                        Text("Free", style = MaterialTheme.typography.bodyMedium)
                    }
                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !state.isFree, onClick = { component.onIsFreeChanged(false) })
                        Text("Paid", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (!state.isFree) {
                OutlinedTextField(
                    value = state.price?.toString() ?: "",
                    onValueChange = { component.onPriceChanged(it.toDoubleOrNull()) },
                    label = { Text("Price (INR)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Payments, null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (state.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.error!!, 
                        color = MaterialTheme.colorScheme.error, 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Button(
                onClick = { component.onCreateClick() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("PUBLISH EVENT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(top = 12.dp)
    )
}
