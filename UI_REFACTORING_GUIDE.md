# PeakFlow UI Refactoring Guide

## Overview

This document outlines the UI modernization performed on PeakFlow to achieve a premium "Sport-Tech" aesthetic. All future screens should follow these patterns for consistency.

---

## Core Design Principles

### 1. Visual Hierarchy
- **Cards over Lists**: Replace dividers with elevated cards (`RoundedCornerShape(24dp)`) and vertical spacing (`12-16dp`).
- **Elevation**: Use subtle borders (`BorderStroke(1.dp, outlineVariant.copy(alpha = 0.5f))`) instead of heavy shadows.
- **Grouping**: Related actions are grouped in surfaces with rounded corners.

### 2. Spacing System
- **Horizontal padding**: `20-24.dp` (screenHorizontal)
- **Vertical spacing**: Use `sectionGap (24.dp)`, `elementGap (12.dp)`, `cardPadding (16.dp)`
- **Corner radii**: 
  - Main cards: `24.dp`
  - Buttons & Inputs: `16.dp`
  - Chips & Small surfaces: `12.dp`

### 3. Typography Scale
```kotlin
// Headlines
MaterialTheme.typography.headlineMedium.copy(
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.5).sp
)

// Section headers
MaterialTheme.typography.labelLarge.copy(
    fontWeight = FontWeight.Black,
    letterSpacing = 1.2.sp,
    color = MaterialTheme.colorScheme.primary
)

// Body text
MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)

// Mono for codes
FontFamily.Monospace + letterSpacing = 2.sp
```

### 4. Color Usage
- **Primary**: Use for interactive elements (buttons, active states)
- **Surface**: Cards use `surface` with subtle borders
- **Background**: Always set `containerColor = MaterialTheme.colorScheme.background` on Scaffold
- **Error**: Use `errorContainer` for error surfaces

---

## Component Patterns

### Modern Card
```kotlin
Card(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp)),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
) {
    // Content with 16.dp padding
}
```

### Elevated Button
```kotlin
Button(
    onClick = { ... },
    modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
    shape = RoundedCornerShape(16.dp),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
) {
    Text(
        "ACTION",
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}
```

### Outlined Text Field (Modern)
```kotlin
OutlinedTextField(
    value = state.value,
    onValueChange = { ... },
    label = { Text("Label") },
    leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp)) },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
)
```

### Pill Action Button (Surface)
```kotlin
Surface(
    onClick = { ... },
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    modifier = Modifier.height(36.dp)
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.Favorite, ...)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Count", style = MaterialTheme.typography.labelLarge)
    }
}
```

### Settings Toggle Item
```kotlin
Surface(
    modifier = Modifier.padding(horizontal = 20.dp),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text("Setting", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)) },
        leadingContent = {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        },
        trailingContent = { Switch(...) }
    )
}
```

### Decorative Background Glow
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    // Background glow
    Box(
        modifier = Modifier
            .size(300.dp)
            .offset(x = 200.dp, y = (-100).dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
    )
    
    // Content here
}
```

---

## Screen-Specific Patterns

### MainScreen with Animated Transitions
```kotlin
Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            NavigationBar(
                tonalElevation = 0.dp,
                containerColor = Color.Transparent,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Nav items with enhanced colors
            }
        }
    }
) { padding ->
    AnimatedContent(
        targetState = state.selectedTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
        },
        label = "tab_transition"
    ) { tab ->
        when (tab) {
            // Screens
        }
    }
}
```

### Detail Screen with Header Image
```kotlin
Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        )
                    )
                )
        )
        
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Spacer(modifier = Modifier.height(140.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                // Content
            }
        }
        
        // Floating bottom actions
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            // Actions
        }
    }
}
```

### List Screen with Pull-to-Refresh
```kotlin
PullToRefreshBox(
    isRefreshing = state.isRefreshing,
    onRefresh = { component.onRefresh() },
    modifier = Modifier.fillMaxSize()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            // Modern card
        }
    }
}
```

---

## File-by-File Refactor Summary

### Completed Screens

1. **FeedScreen.kt**
   - Changed from list with dividers to card-based layout
   - Added interactive pill-shaped like/comment buttons
   - Improved image display with better clipping

2. **EventsListScreen.kt**
   - Modern card design with borders
   - Category filter chips with circular shape
   - Participant count in header
   - Enhanced RSVP button states

3. **CommunitiesListScreen.kt**
   - SecondaryScrollableTabRow with custom indicator
   - Modern card layout with join action surface
   - Improved spacing and typography

4. **ProfileScreen.kt**
   - Dashboard-style with profile card
   - Verified badge on avatar
   - Color-coded stat cards
   - Interests as bordered chips

5. **WelcomeScreen.kt**
   - Animated page indicators
   - Decorative radial gradient background
   - Primary-colored CTA button with arrow icon
   - Multi-layered icon design

6. **SignInScreen.kt / SignUpScreen.kt**
   - Decorative background elements
   - Enhanced text fields with focused borders
   - Improved error display in surfaces
   - Consistent button styling

7. **SplashScreen.kt**
   - Pulsing scale animation on logo
   - Gradient background
   - Tagline with letter spacing

8. **PostDetailScreen.kt**
   - Post content in surface card
   - Sticky comment input with elevation
   - Empty comment state with icon

9. **EventDetailScreen.kt**
   - Parallax-style header gradient
   - Logistics grid with icon surfaces
   - Floating bottom action bar
   - Progress indicator with custom colors

10. **CommunityDetailScreen.kt**
    - Scrollable tabs with secondary indicator
    - Admin FAB with extended actions
    - Modern member list items
    - Enhanced empty states

11. **CreatePostScreen.kt**
    - Large text area with rounded corners
    - Modern image attachment surface
    - Elevated primary button

12. **SettingsScreen.kt**
    - Grouped setting surfaces
    - Icon containers with primary tint
    - Outlined logout button
    - Enhanced dialog design

13. **CreateEventScreen.kt**
    - Section headers for organization
    - Leading icons in all inputs
    - Radio group in surfaced container
    - Modern date/time inputs

14. **EditProfileScreen.kt / ProfileSetupScreen.kt**
    - Large avatar picker with border
    - Camera badge overlay
    - Enhanced interest selection chips
    - Decorative background glow

15. **OtpVerificationScreen.kt**
    - Large OTP input with wide letter spacing
    - Decorative background
    - Improved countdown button

16. **InviteCodeScreen.kt**
    - Premium code display with mono font
    - Enhanced input styling
    - Modern icon treatment

17. **GenerateInviteScreen.kt**
    - Large display typography for generated code
    - Slider with custom colors
    - Modern existing code list items

18. **JoinRequestsScreen.kt**
    - Pill action buttons for approve/reject
    - Elevated card design
    - Improved empty state

---

## Migration Checklist for New Screens

- [ ] Scaffold has `containerColor = MaterialTheme.colorScheme.background`
- [ ] TopAppBar uses `TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)`
- [ ] Cards use `RoundedCornerShape(24.dp)` with border stroke
- [ ] Buttons use `RoundedCornerShape(16.dp)` with proper elevation
- [ ] Text fields use `RoundedCornerShape(16.dp)` with custom colors
- [ ] All screens have consistent horizontal padding (20-24.dp)
- [ ] Empty states include icon, title, message, and optional action
- [ ] Loading states use `CircularProgressIndicator` with appropriate strokeWidth
- [ ] Error states displayed in surfaced containers with error color
- [ ] Pull-to-refresh implemented where appropriate
- [ ] Tab transitions use `AnimatedContent` with fade
- [ ] Navigation bar has elevation and enhanced colors
- [ ] Decorative background glows used sparingly on auth/onboarding screens

---

## Design Tokens

```kotlin
// Spacing
val screenHorizontal = 20.dp
val sectionGap = 24.dp
val elementGap = 12.dp
val cardPadding = 16.dp

// Corner Radii
val cardRadius = 24.dp
val buttonRadius = 16.dp
val chipRadius = 12.dp

// Elevation
val cardElevation = 0.5.dp
val buttonElevation = 2.dp
val bottomNavElevation = 8.dp

// Typography
val headlineLetterSpacing = (-0.5).sp
val sectionLetterSpacing = 1.2.sp
val monoLetterSpacing = 2.sp
```

---

## Conclusion

All future UI development should adhere to these patterns. When in doubt, reference the recently refactored screens as templates. Consistency is key to maintaining the premium PeakFlow brand experience.

**Last Updated**: December 2024
**Version**: 2.0 (Premium Refactor)
