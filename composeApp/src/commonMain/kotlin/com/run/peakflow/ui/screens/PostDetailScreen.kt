package com.run.peakflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.PostComment
import com.run.peakflow.presentation.components.PostDetailComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(component: PostDetailComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post Thread", style = PeakFlowTypography.bodyTitle()) },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
        if (state.post != null) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
                item {
                    Column(modifier = Modifier.padding(PeakFlowSpacing.screenHorizontal)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp))
                            }
                            Spacer(modifier = Modifier.width(PeakFlowSpacing.elementGap))
                            Column {
                                Text(state.post!!.authorName, style = PeakFlowTypography.bodyTitle())
                                if (state.community != null) {
                                    Text(state.community!!.title, style = PeakFlowTypography.labelSecondary(), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
                        Text(state.post!!.content, style = PeakFlowTypography.bodyMain())
                        
                        Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.hasLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (state.hasLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp).clickable { component.onLikeClick() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${state.post!!.likesCount} likes", style = PeakFlowTypography.labelSecondary())
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                item {
                    Text(
                        text = "COMMENTS",
                        style = PeakFlowTypography.sectionHeader(),
                        modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
                    )
                }

                items(state.comments, key = { it.id }) { comment ->
                    CommentItem(comment = comment)
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: PostComment) {
    ListItem(
        headlineContent = { Text(comment.userName, style = PeakFlowTypography.bodyTitle()) },
        supportingContent = { Text(comment.content, style = PeakFlowTypography.bodyMain()) },
        leadingContent = {
            Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(Icons.Default.Person, null, modifier = Modifier.padding(6.dp))
            }
        }
    )
}

@Composable
private fun CommentInput(
    commentText: String,
    isLoading: Boolean,
    onCommentTextChanged: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(PeakFlowSpacing.elementGap).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = commentText,
                onValueChange = onCommentTextChanged,
                placeholder = { Text("Say something...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSendClick, enabled = !isLoading && commentText.isNotBlank()) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
