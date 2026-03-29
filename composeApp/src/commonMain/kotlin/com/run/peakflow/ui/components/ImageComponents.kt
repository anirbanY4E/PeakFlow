package com.run.peakflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Avatar image component with placeholder support.
 * Uses Coil3 for async image loading.
 *
 * @param imageUrl URL of the avatar image (null for placeholder)
 * @param imageBytes Optional byte array for locally picked images (takes precedence over imageUrl)
 * @param modifier Modifier for the component
 * @param size Size of the avatar in dp
 * @param contentDescription Content description for accessibility
 */
@Composable
fun AvatarImage(
    imageUrl: String?,
    imageBytes: ByteArray? = null,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        when {
            imageBytes != null -> {
                // Display local image from bytes
                SubcomposeAsyncImage(
                    model = imageBytes,
                    contentDescription = contentDescription ?: "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(size * 0.5f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(size * 0.2f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            imageUrl != null -> {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription ?: "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(size * 0.5f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(size * 0.2f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = contentDescription ?: "Default Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.2f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Post image component for displaying post images.
 *
 * @param imageUrl URL of the post image
 * @param modifier Modifier for the component
 * @param contentDescription Content description for accessibility
 */
@Composable
fun PostImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription ?: "Post image",
            modifier = modifier.clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Community image component for displaying community images.
 *
 * @param imageUrl URL of the community image
 * @param emoji Emoji to display as fallback
 * @param modifier Modifier for the component
 * @param contentDescription Content description for accessibility
 */
@Composable
fun CommunityImage(
    imageUrl: String?,
    emoji: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription ?: "Community image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (emoji != null) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (emoji != null) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (emoji != null) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}
