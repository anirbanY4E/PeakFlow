package com.run.peakflow.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Oops!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun EmptyView(
    modifier: Modifier = Modifier,
    title: String = "Nothing here yet",
    message: String = "",
    icon: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            if (message.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (action != null) {
                Spacer(modifier = Modifier.height(24.dp))
                action()
            }
        }
    }
}
