package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.CreatePostComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(component: CreatePostComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Post", style = PeakFlowTypography.bodyTitle()) },
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
        ) {
            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

            OutlinedTextField(
                value = state.content,
                onValueChange = { component.onContentChanged(it) },
                label = { Text("Share with community") },
                placeholder = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

            OutlinedTextField(
                value = state.imageUrl ?: "",
                onValueChange = { component.onImageUrlChanged(it.ifBlank { null }) },
                label = { Text("Image URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Button(
                onClick = { component.onCreateClick() },
                enabled = !state.isLoading && state.content.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("POST TO COMMUNITY")
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
        }
    }
}
