package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.PostComment
import com.run.peakflow.presentation.components.PostDetailComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(component: PostDetailComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.post != null) {
                CommentInput(
                    commentText = state.commentText,
                    isLoading = state.isCommentLoading,
                    onCommentTextChanged = { component.onCommentTextChanged(it) },
                    onSendClick = { component.onSendCommentClick() }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading && state.post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            state.post != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                ) {
                    // Post content
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Author
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.padding(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = state.post!!.authorName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (state.community != null) {
                                        Text(
                                            text = state.community!!.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Content
                            Text(
                                text = state.post!!.content,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Actions
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { component.onLikeClick() },
                                    enabled = !state.isLikeLoading
                                ) {
                                    if (state.isLikeLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    } else {
                                        Icon(
                                            imageVector = if (state.hasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (state.hasLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${state.post!!.likesCount} likes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.width(24.dp))

                                Text(
                                    text = "${state.post!!.commentsCount} comments",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()
                    }

                    // Comments header
                    item {
                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Comments list
                    if (state.comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No comments yet. Be the first!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(state.comments, key = { it.id }) { comment ->
                            CommentItem(comment = comment)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: PostComment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun CommentInput(
    commentText: String,
    isLoading: Boolean,
    onCommentTextChanged: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChanged,
                placeholder = { Text("Write a comment...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = !isLoading && commentText.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (commentText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}