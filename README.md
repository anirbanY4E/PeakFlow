# PeakFlow: Production Readiness Roadmap

PeakFlow is a Kotlin Multiplatform (KMP) fitness community platform. This document outlines the technical transition from the current MVP (Mock API) to a production-grade, reactive architecture with a Django backend.

---

## 🚀 Phase 2: Production Transition Plan

### 1. Backend Architecture (Django)
The backend will be migrated to **Django REST Framework (DRF)** to leverage its robust ORM and authentication ecosystem.

*   **Database**: PostgreSQL for relational data (Users, Communities, Memberships).
*   **Authentication**: 
    *   JWT (JSON Web Tokens) for stateless API access.
    *   Django-allauth for social (Google) integration.
*   **Real-time Reactivity**: 
    *   **Django Channels**: To implement WebSockets for instant updates.
    *   **Redis**: As a message broker to handle real-time event broadcasting (e.g., "New Post in Cubbon Park Runners").

### 2. KMP Data Layer Refactor
To ensure the app is state-aware and offline-capable, the KMP data layer will move to a **Repository Pattern with Local Cache**.

*   **SQLDelight**: Implementation of a local SQLite database in `shared`.
*   **Ktor Client**: Replace `MockApiService` with a real Ktor implementation.
*   **Single Source of Truth (SSOT)**:
    *   Repositories will return `Flow<List<Data>>`.
    *   The Flow will emit data from the local database immediately.
    *   A background network sync will update the database, which automatically triggers a UI update via the Flow.

### 3. Reactive Event System
For a production-grade "live" feel, we will implement a tiered reactivity model:

| Trigger | Mechanism | UX Result |
| :--- | :--- | :--- |
| **User Action** | Optimistic UI | Immediate feedback (e.g., heart turns red instantly). |
| **Local Sync** | Repository Flow | Data persists and is consistent across all screens. |
| **Global Update** | WebSockets (Ktor + Channels) | Instant notification when an Admin approves a join request or an event is created nearby. |

---

## 🛠️ Feature-Specific Production Goals

### For Members
*   **Reactive Feed**: New posts appear in the feed without manual refresh via WebSocket signals.
*   **Push Notifications**: Integration with Firebase Cloud Messaging (FCM) for event reminders and community messages.
*   **Offline Mode**: Full read access to joined communities and booked events without an active internet connection.

### For Admins
*   **Real-time Moderation**: Join requests appear instantly via a "Pending" badge update.
*   **Analytics Dashboard**: Django-driven insights into community growth and event attendance rates.
*   **Invite Management**: Dynamic link generation for invite codes that deep-link directly into the app.

---

## 📐 Standardized UI/UX Specs (Industry Standard)
The app now follows a strictly enforced **Design System** located in `shared/theme`:

*   **Ratios**: 20dp screen margins, 12dp element gaps, 24dp section gaps.
*   **Typography**: Defined hierarchy from `screenTitle` (HeadlineMedium) to `labelSecondary` (LabelSmall).
*   **Consistency**: Shared button heights (56dp) and corner radii (12dp) across all 15+ screens.

---

## 🏗️ Technical Stack (Production)
*   **Mobile**: Kotlin Multiplatform (Android & iOS)
*   **UI**: Jetpack Compose / Compose Multiplatform
*   **Navigation**: Decompose (Component-based routing)
*   **DI**: Koin
*   **Backend**: Python / Django / DRF
*   **Real-time**: Django Channels / WebSockets
*   **Database**: PostgreSQL + Redis
*   **Storage**: AWS S3 (for post and community images)

---

## 🏃 Getting Started (MVP Development)
```bash
# Build Android
.\gradlew.bat :composeApp:assembleDebug

# Run Unit Tests
.\gradlew.bat :shared:test
```

*Current Status: MVP with Uniform UI & Reactive State handling.*
