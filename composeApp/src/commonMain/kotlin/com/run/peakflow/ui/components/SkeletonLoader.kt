package com.run.peakflow.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A reusable shimmer effect for skeleton loading states.
 * Usage: Wrap any content with this modifier to apply the shimmer animation.
 */
val shimmerModifier = Modifier.background(
    brush = Brush.linearGradient(
        colors = listOf(
            Color.Gray.copy(alpha = 0.2f),
            Color.Gray.copy(alpha = 0.4f),
            Color.Gray.copy(alpha = 0.2f)
        ),
        start = Offset(-1000f, 0f),
        end = Offset(1000f, 0f)
    )
)

/**
 * Animated shimmer effect that cycles across the content.
 * Use this for skeleton placeholders that should have a moving highlight.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.2f),
        Color.Gray.copy(alpha = 0.5f),
        Color.Gray.copy(alpha = 0.2f)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateX = infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateX.value - 500, 0f),
        end = Offset(translateX.value + 500, 0f)
    )
}

/**
 * Skeleton base with animated shimmer. Most skeleton components should use this as a wrapper.
 */
@Composable
fun SkeletonBase(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp)
) {
    val shimmerBrush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .background(brush = shimmerBrush, shape = shape)
    )
}

// ========== Specific Skeleton Components ==========

/**
 * Skeleton that matches the shape of a PostCard in FeedScreen/CommunityDetailScreen.
 */
@Composable
fun PostCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header: avatar + name + community
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBase(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonBase(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(14.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonBase(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content lines
        repeat(3) {
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Optional image placeholder
        SkeletonBase(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Actions row: like + comment buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBase(
                modifier = Modifier
                    .size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            SkeletonBase(
                modifier = Modifier
                    .size(48.dp)
            )
        }
    }
}

/**
 * Skeleton that matches the shape of an EventCard in EventsListScreen/EventsTab.
 */
@Composable
fun EventCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Category badge + participants
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBase(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 12.dp)
            )
            SkeletonBase(
                modifier = Modifier
                    .height(16.dp)
                    .width(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        SkeletonBase(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(22.dp)
        )

        // Date & location lines
        repeat(2) {
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CTA Button placeholder
        SkeletonBase(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    }
}

/**
 * Skeleton that matches the shape of a CommunityCard in CommunitiesListScreen.
 */
@Composable
fun CommunityCardSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Community image with small surface radius (14.dp matches actual CommunityCard)
        SkeletonBase(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
            )
        }
        // Optional trailing icon placeholder (for join/arrow)
        SkeletonBase(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

/**
 * Skeleton for the Profile screen (header card, stats, interests).
 */
@Composable
fun ProfileSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Profile card with 28.dp corner radius (matches actual ProfileScreen Card)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment =Alignment.CenterHorizontally
            ) {
                SkeletonBase(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(50.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonBase(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonBase(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats row with cards using 20.dp corner radius
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SkeletonBase(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonBase(modifier = Modifier.fillMaxWidth(0.6f).height(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonBase(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Interests section title
        SkeletonBase(modifier = Modifier.fillMaxWidth(0.3f).height(24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                SkeletonBase(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

/**
 * Skeleton for a MemberItem in CommunityDetail Members tab.
 * Matches ListItem with default padding (16.dp).
 */
@Composable
fun MemberItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp), // Match default ListItem horizontal padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBase(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
            )
        }
        // Optional admin badge placeholder
        SkeletonBase(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}

/**
 * Skeleton for a Comment in PostDetailScreen.
 */
@Composable
fun CommentSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp), // Match PostDetail comment padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBase(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBase(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )
        }
    }
}