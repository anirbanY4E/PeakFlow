package com.run.peakflow.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.run.peakflow.data.models.Post
import com.run.peakflow.presentation.components.FeedComponent
import com.run.peakflow.ui.components.EmptyView
import com.run.peakflow.ui.components.ErrorView
import com.run.peakflow.ui.components.LoadingView
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    component: FeedComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && state.posts.isEmpty() -> LoadingView()
            state.error != null && state.posts.isEmpty() -> ErrorView(message = state.error!!, onRetry = { component.loadFeed() })
            state.posts.isEmpty() -> EmptyView(title = "No posts yet")
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { component.onRefresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "Your Feed",
                                style = PeakFlowTypography.screenTitle(),
                                modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal, vertical = PeakFlowSpacing.sectionGap)
                            )
                        }

                        items(state.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                isLiked = post.id in state.likedPostIds,
                                onPostClick = { component.onPostClick(post.id) },
                                onLikeClick = { component.onLikeClick(post.id) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = PeakFlowSpacing.screenHorizontal),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(post: Post, isLiked: Boolean, onPostClick: () -> Unit, onLikeClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPostClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(PeakFlowSpacing.screenHorizontal)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(PeakFlowSpacing.elementGap))
                Column {
                    Text(post.authorName, style = PeakFlowTypography.bodyTitle())
                    Text("Community Post", style = PeakFlowTypography.labelSecondary(), color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))
            Text(post.content, style = PeakFlowTypography.bodyMain(), maxLines = 5, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("${post.likesCount}", style = PeakFlowTypography.labelSecondary())
                Spacer(modifier = Modifier.width(24.dp))
                Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${post.commentsCount}", style = PeakFlowTypography.labelSecondary())
            }
        }
    }
}
