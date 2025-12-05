package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.presentation.components.CreateEventComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(component: CreateEventComponent) {
    val state by component.state.collectAsState()
    var categoryExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = { component.onTitleChanged(it) },
                label = { Text("Event Title *") },
                placeholder = { Text("e.g., Saturday Morning 5K Run") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Category
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
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    EventCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text("${category.emoji} ${category.displayName}") },
                            onClick = {
                                component.onCategoryChanged(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Date
            OutlinedTextField(
                value = state.date,
                onValueChange = { component.onDateChanged(it) },
                label = { Text("Date *") },
                placeholder = { Text("e.g., Dec 14, 2025") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Time
            OutlinedTextField(
                value = state.time,
                onValueChange = { component.onTimeChanged(it) },
                label = { Text("Start Time *") },
                placeholder = { Text("e.g., 6:30 AM") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Location
            OutlinedTextField(
                value = state.location,
                onValueChange = { component.onLocationChanged(it) },
                label = { Text("Location *") },
                placeholder = { Text("e.g., Cubbon Park East Gate") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { component.onDescriptionChanged(it) },
                label = { Text("Description") },
                placeholder = { Text("Tell participants what to expect...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Max Participants
            OutlinedTextField(
                value = state.maxParticipants.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { max -> component.onMaxParticipantsChanged(max) }
                },
                label = { Text("Max Participants") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Free or Paid
            Text(
                text = "Event Type",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.isFree,
                        onClick = { component.onIsFreeChanged(true) }
                    )
                    Text("Free")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !state.isFree,
                        onClick = { component.onIsFreeChanged(false) }
                    )
                    Text("Paid")
                }
            }

            // Price (if paid)
            if (!state.isFree) {
                OutlinedTextField(
                    value = state.price?.toString() ?: "",
                    onValueChange = {
                        component.onPriceChanged(it.toDoubleOrNull())
                    },
                    label = { Text("Price") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Error message
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Create button
            Button(
                onClick = { component.onCreateClick() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("CREATE EVENT")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
