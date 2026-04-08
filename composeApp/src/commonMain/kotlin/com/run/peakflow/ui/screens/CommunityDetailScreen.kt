package com.run.peakflow.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.models.Post
import com.run.peakflow.presentation.components.CommunityDetailComponent
import com.run.peakflow.presentation.state.CommunityTab
import com.run.peakflow.presentation.state.MemberWithUser
import com.run.peakflow.ui.components.*
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(component: CommunityDetailComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.community?.title ?: "Community",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.community != null) {
                            Text(
                                text = "${state.community!!.memberCount} members • ${state.community!!.city}",
                                style = MaterialTheme.typography.labelSmall,
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
                            Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "Requests")
                        }
                    }
                    if (state.userRole != null) {
                        IconButton(onClick = { component.onGenerateInviteClick() }) {
                            Icon(imageVector = Icons.Default.IosShare, contentDescription = "Invite", modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Button(
                            onClick = { component.onJoinCommunityClick() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            enabled = !state.hasPendingJoinRequest && !state.isJoining
                        ) {
                            if (state.isJoining) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (state.hasPendingJoinRequest) "Requested" else "Join",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (state.hasPendingJoinRequest) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
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
                // Show skeleton loaders for initial community data load
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // TopAppBar skeleton placeholder (simplified version)
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SkeletonBase(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    SkeletonBase(modifier = Modifier.fillMaxWidth(0.5f).height(20.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SkeletonBase(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp))
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {}) {
                                SkeletonBase(modifier = Modifier.size(24.dp))
                            }
                        },
                        actions = {
                            repeat(2) {
                                IconButton(onClick = { }) {
                                    SkeletonBase(modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // TabRow
                    ScrollableTabRow(
                        selectedTabIndex = state.selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        divider = {},
                        edgePadding = 20.dp,
                        indicator = { tabPositions ->
                            if (state.selectedTab.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        CommunityTab.entries.forEach { tab ->
                            val selected = state.selectedTab == tab
                            Tab(
                                selected = selected,
                                onClick = { /* disabled during loading */ },
                                text = {
                                    Text(
                                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Content skeleton based on selected tab
                    when (state.selectedTab) {
                        CommunityTab.POSTS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(6) {
                                    PostCardSkeleton()
                                }
                            }
                        }
                        CommunityTab.EVENTS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(5) {
                                    EventCardSkeleton()
                                }
                            }
                        }
                        CommunityTab.MEMBERS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                            ) {
                                items(6) {
                                    MemberItemSkeleton()
                                }
                            }
                        }
                        CommunityTab.ABOUT -> {
                            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                SkeletonBase(modifier = Modifier.fillMaxWidth().height(24.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                repeat(4) {
                                    SkeletonBase(modifier = Modifier.fillMaxWidth().height(16.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
            state.community != null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Modern Tab Bar
                    ScrollableTabRow(
                        selectedTabIndex = state.selectedTab.ordinal,
                        containerColor = Color.Transparent,
                        divider = {},
                        edgePadding = 20.dp,
                        indicator = { tabPositions ->
                            if (state.selectedTab.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.ordinal]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        CommunityTab.entries.forEach { tab ->
                            val selected = state.selectedTab == tab
                            Tab(
                                selected = selected,
                                onClick = { component.onTabSelected(tab) },
                                text = {
                                    Text(
                                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                                isPostsLoadingMore = state.isPostsLoadingMore,
                                hasMorePosts = state.hasMorePosts,
                                onLoadMore = { component.loadMorePosts() },
                                onPostClick = { component.onPostClick(it) },
                                onLikeClick = { component.onLikePostClick(it) }
                            )
                            CommunityTab.EVENTS -> EventsTabContent(
                                events = state.events,
                                rsvpedEventIds = state.rsvpedEventIds,
                                rsvpingEventIds = state.rsvpingEventIds,
                                userRole = state.userRole,
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
private fun PostsTabContent(
    posts: List<Post>,
    likedPostIds: Set<String>,
    isPostsLoadingMore: Boolean,
    hasMorePosts: Boolean,
    onLoadMore: () -> Unit,
    onPostClick: (String) -> Unit,
    onLikeClick: (String) -> Unit
) {
    val uniquePosts = remember(posts) { posts.distinctBy { it.id } }
    if (uniquePosts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DynamicFeed, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No posts yet in this community", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()

        LaunchedEffect(listState, hasMorePosts, isPostsLoadingMore) {
            androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                .collect { lastIndex ->
                    if (lastIndex != null && lastIndex >= uniquePosts.size - 2) {
                        if (hasMorePosts && !isPostsLoadingMore) {
                            onLoadMore()
                        }
                    }
                }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uniquePosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    isLiked = post.id in likedPostIds,
                    onPostClick = { onPostClick(post.id) },
                    onLikeClick = { onLikeClick(post.id) }
                )
            }
            if (isPostsLoadingMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsTabContent(
    events: List<com.run.peakflow.data.models.Event>,
    rsvpedEventIds: Set<String>,
    rsvpingEventIds: Set<String>,
    userRole: MembershipRole?,
    onEventClick: (String) -> Unit,
    onRsvpClick: (String) -> Unit
) {
    val uniqueEvents = remember(events) { events.distinctBy { it.id } }
    if (uniqueEvents.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.EventNote, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No upcoming events", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uniqueEvents, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    isRsvped = event.id in rsvpedEventIds,
                    onEventClick = { onEventClick(event.id) },
                    onRsvpClick = { onRsvpClick(event.id) },
                    canRsvp = userRole != null,
                    isRsvping = event.id in rsvpingEventIds
                )
            }
        }
    }
}

@Composable
private fun MembersTabContent(members: List<MemberWithUser>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        items(members, key = { it.membership.id }) { memberWithUser ->
            MemberItemModern(memberWithUser = memberWithUser)
        }
    }
}

@Composable
private fun MemberItemModern(memberWithUser: MemberWithUser) {
    val user = memberWithUser.user
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Member"
    
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(displayName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        },
        supportingContent = {
            Text(user?.city ?: "PeakFlow Member", style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            AvatarImage(
                imageUrl = user?.avatarUrl,
                size = 44.dp,
                contentDescription = "$displayName avatar"
            )
        },
        trailingContent = {
            if (memberWithUser.membership.role == MembershipRole.ADMIN) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "Admin",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

@Composable
private fun AboutTabContent(community: com.run.peakflow.data.models.CommunityGroup) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Community Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = community.description,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        
        if (community.rules.isNotEmpty()) {
            item {
                Text(
                    text = "Guidelines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                community.rules.forEachIndexed { i, rule ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = "${i + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = rule,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminActionsFab(onCreateEventClick: () -> Unit, onCreatePostClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    val rotation by animateFloatAsState(if (expanded) 45f else 0f)
    
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(visible = expanded) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { expanded = false; onCreatePostClick() },
                    icon = { Icon(Icons.Default.Edit, "Post") },
                    text = { Text("Create Post") },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                ExtendedFloatingActionButton(
                    onClick = { expanded = false; onCreateEventClick() },
                    icon = { Icon(Icons.Default.Event, "Event") },
                    text = { Text("Create Event") },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}