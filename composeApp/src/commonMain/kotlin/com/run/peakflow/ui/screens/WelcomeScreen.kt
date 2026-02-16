package com.run.peakflow.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.WelcomeComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Groups,
        title = "Discover Your Tribe",
        description = "Join local fitness communities in your city. Running, trekking, cycling, and more."
    ),
    OnboardingPage(
        icon = Icons.Default.Event,
        title = "Real Events, Real People",
        description = "Attend events organized by your communities. Meet like-minded fitness enthusiasts."
    ),
    OnboardingPage(
        icon = Icons.Default.CardGiftcard,
        title = "Earn As You Move",
        description = "Get rewards and perks for participating. The more active you are, the more you earn."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(component: WelcomeComponent) {
    val state by component.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            component.onPageChanged(page)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PeakFlowSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(0.7f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPageContent(onboardingPages[page])
        }

        // Page indicators
        Row(
            modifier = Modifier.padding(vertical = PeakFlowSpacing.sectionGap),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(onboardingPages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == state.currentPage) 20.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == state.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            }
                        )
                )
            }
        }

        Button(
            onClick = { component.onGetStartedClick() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "GET STARTED",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

        TextButton(onClick = { component.onSignInClick() }) {
            Text(
                text = "Already have an account? Sign In",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

        Text(
            text = page.title,
            style = PeakFlowTypography.screenTitle(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

        Text(
            text = page.description,
            style = PeakFlowTypography.bodyMain(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
