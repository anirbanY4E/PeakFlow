package com.run.peakflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.models.Post
import com.run.peakflow.presentation.components.CommunityDetailComponent
import com.run.peakflow.presentation.state.CommunityTab
import com.run.peakflow.presentation.state.MemberWithUser
import com.run.peakflow.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(component: CommunityDetailComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.community?.title ?: "Community",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.community != null) {
                            Text(
                                text = "${state.community!!.memberCount} members",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.userRole == MembershipRole.ADMIN) {
                        IconButton(onClick = { component.onJoinRequestsClick() }) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Requests")
                        }
                    }
                    IconButton(onClick = { component.onGenerateInviteClick() }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Invite")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (state.userRole == MembershipRole.ADMIN) {
                AdminActionsFab(
                    onCreateEventClick = { component.onCreateEventClick() },
                    onCreatePostClick = { component.onCreatePostClick() }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading && state.community == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.community != null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Compact Header instead of big block
                    CompactCommunityInfo(description = state.community!!.description)

                    // Tabs (Reduced height)
                    TabRow(
                        selectedTabIndex = state.selectedTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surface,
                        divider = { HorizontalDivider(thickness = 0.5.dp) }
                    ) {
                        CommunityTab.entries.forEach { tab ->
                            Tab(
                                selected = state.selectedTab == tab,
                                onClick = { component.onTabSelected(tab) },
                                text = { 
                                    Text(
                                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge
                                    ) 
                                }
                            )
                        }
                    }

                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { component.onRefresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (state.selectedTab) {
                            CommunityTab.POSTS -> PostsTabContent(
                                posts = state.posts,
                                likedPostIds = state.likedPostIds,
                                onPostClick = { component.onPostClick(it) },
                                onLikeClick = { component.onLikePostClick(it) }
                            )
                            CommunityTab.EVENTS -> EventsTabContent(
                                events = state.events,
                                rsvpedEventIds = state.rsvpedEventIds,
                                onEventClick = { component.onEventClick(it) },
                                onRsvpClick = { component.onRsvpEventClick(it) }
                            )
                            CommunityTab.MEMBERS -> MembersTabContent(members = state.members)
                            CommunityTab.ABOUT -> AboutTabContent(community = state.community!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactCommunityInfo(description: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PostsTabContent(
    posts: List<Post>,
    likedPostIds: Set<String>,
    onPostClick: (String) -> Unit,
    onLikeClick: (String) -> Unit
) {
    // UI-layer dedup: ultimate defense against duplicate key crash from concurrent state updates
    val uniquePosts = remember(posts) { posts.distinctBy { it.id } }
    if (uniquePosts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No posts yet", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uniquePosts, key = { it.id }) { post ->
                CommunityPostCard(
                    post = post,
                    isLiked = post.id in likedPostIds,
                    onPostClick = { onPostClick(post.id) },
                    onLikeClick = { onLikeClick(post.id) }
                )
            }
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: Post,
    isLiked: Boolean,
    onPostClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPostClick() },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    imageUrl = post.authorAvatarUrl,
                    size = 32.dp,
                    contentDescription = "${post.authorName}'s avatar"
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(post.authorName, style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(post.content, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
            
            // Display post image if available
            if (post.imageUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                coil3.compose.AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).clickable { onLikeClick() }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("${post.likesCount}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${post.commentsCount}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun EventsTabContent(
    events: List<com.run.peakflow.data.models.Event>,
    rsvpedEventIds: Set<String>,
    onEventClick: (String) -> Unit,
    onRsvpClick: (String) -> Unit
) {
    val uniqueEvents = remember(events) { events.distinctBy { it.id } }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(uniqueEvents, key = { it.id }) { event ->
            EventCard(
                event = event,
                isRsvped = event.id in rsvpedEventIds,
                onEventClick = { onEventClick(event.id) },
                onRsvpClick = { onRsvpClick(event.id) }
            )
        }
    }
}

@Composable
private fun MembersTabContent(members: List<MemberWithUser>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        items(members, key = { it.membership.id }) { memberWithUser ->
            MemberItem(memberWithUser = memberWithUser)
        }
    }
}

@Composable
private fun MemberItem(memberWithUser: MemberWithUser) {
    val membership = memberWithUser.membership
    val user = memberWithUser.user
    // Format the timestamp to a readable date string
    val joinedDate = remember(membership.joinedAt) {
        formatTimestampToDate(membership.joinedAt)
    }
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Member"
    
    ListItem(
        headlineContent = { Text(displayName, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = { Text("Joined $joinedDate", style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            AvatarImage(
                imageUrl = user?.avatarUrl,
                size = 36.dp,
                contentDescription = "$displayName avatar"
            )
        },
        trailingContent = {
            if (membership.role == MembershipRole.ADMIN) {
                SuggestionChip(onClick = {}, label = { Text("Admin") })
            }
        }
    )
}

/**
 * Formats a timestamp (milliseconds since epoch) to a readable relative date string
 */
private fun formatTimestampToDate(timestamp: Long): String {
    return if (timestamp > 0) {
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        val diffSeconds = diffMs / 1000
        val diffDays = diffSeconds / 86400
        val diffYears = diffDays / 365
        val diffMonths = (diffDays % 365) / 30
        val diffWeeks = diffDays / 7
        
        when {
            diffYears > 0 -> "$diffYears year${if (diffYears > 1) "s" else ""} ago"
            diffMonths > 0 -> "$diffMonths month${if (diffMonths > 1) "s" else ""} ago"
            diffWeeks > 0 -> "$diffWeeks week${if (diffWeeks > 1) "s" else ""} ago"
            diffDays > 0 -> "$diffDays day${if (diffDays > 1) "s" else ""} ago"
            else -> "Recently"
        }
    } else {
        "Recently"
    }
}

@Composable
private fun AboutTabContent(community: com.run.peakflow.data.models.CommunityGroup) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("About", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(community.description, style = MaterialTheme.typography.bodyMedium)
        }
        if (community.rules.isNotEmpty()) {
            item {
                Text("Rules", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                community.rules.forEachIndexed { i, rule ->
                    Text("${i + 1}. $rule", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun AdminActionsFab(onCreateEventClick: () -> Unit, onCreatePostClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(visible = expanded) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = { expanded = false; onCreatePostClick() }) {
                    Icon(Icons.Default.Edit, "Post")
                }
                SmallFloatingActionButton(onClick = { expanded = false; onCreateEventClick() }) {
                    Icon(Icons.Default.Event, "Event")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(if (expanded) Icons.Default.Close else Icons.Default.Add, null)
        }
    }
}
