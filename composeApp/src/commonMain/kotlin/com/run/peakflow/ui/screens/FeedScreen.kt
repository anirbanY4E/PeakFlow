package com.run.peakflow.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.data.models.Post
import com.run.peakflow.presentation.components.FeedComponent
import com.run.peakflow.ui.components.AvatarImage
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
                    val uniquePosts = remember(state.posts) { state.posts.distinctBy { it.id } }
                    val listState = rememberLazyListState()

                    LaunchedEffect(listState, state.hasMorePosts, state.isLoadingMore) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { lastIndex ->
                                if (lastIndex != null && lastIndex >= uniquePosts.size - 2) {
                                    if (state.hasMorePosts && !state.isLoadingMore) {
                                        component.loadMore()
                                    }
                                }
                            }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp) // Space for potential FAB
                    ) {
                        item {
                            Text(
                                text = "Your Feed",
                                style = PeakFlowTypography.screenTitle(),
                                modifier = Modifier.padding(
                                    horizontal = PeakFlowSpacing.screenHorizontal,
                                    vertical = PeakFlowSpacing.sectionGap
                                )
                            )
                        }

                        items(uniquePosts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                isLiked = post.id in state.likedPostIds,
                                onPostClick = { component.onPostClick(post.id) },
                                onLikeClick = { component.onLikeClick(post.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(post: Post, isLiked: Boolean, onPostClick: () -> Unit, onLikeClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PeakFlowSpacing.screenHorizontal)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onPostClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    imageUrl = post.authorAvatarUrl,
                    size = 44.dp,
                    contentDescription = "${post.authorName}'s avatar"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = post.authorName,
                        style = PeakFlowTypography.bodyTitle().copy(fontSize = 15.sp)
                    )
                    Text(
                        text = post.communityName ?: "PeakFlow Member",
                        style = PeakFlowTypography.labelSecondary(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Content
            Text(
                text = post.content,
                style = PeakFlowTypography.bodyMain().copy(
                    lineHeight = 22.sp,
                    letterSpacing = 0.2.sp
                ),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
            
            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(12.dp))
                coil3.compose.AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 300.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onLikeClick,
                    shape = CircleShape,
                    color = if (isLiked) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Surface(
                    onClick = onPostClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.commentsCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
