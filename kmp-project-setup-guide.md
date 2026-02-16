# Kotlin Multiplatform (KMP) Project Setup Guide

A comprehensive guide to setting up a production-ready Kotlin Multiplatform project with shared business logic for Android and iOS.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Module Structure](#module-structure)
3. [Root Project Configuration](#root-project-configuration)
4. [Version Catalog](#version-catalog)
5. [Shared Module Setup](#shared-module-setup)
6. [ComposeApp Module Setup](#composeapp-module-setup)
7. [Architecture Overview](#architecture-overview)
8. [Platform Entry Points](#platform-entry-points)
9. [Building and Running](#building-and-running)

---

## Project Overview

This guide covers setting up a **Kotlin Multiplatform (KMP)** project with the following stack:

| Component | Technology |
|-----------|------------|
| **UI Framework** | Jetpack Compose Multiplatform |
| **Navigation** | Decompose (Component-based) |
| **Dependency Injection** | Koin |
| **Networking** | Ktor Client |
| **Serialization** | Kotlinx Serialization |
| **Build System** | Gradle with Version Catalogs |

### Key Benefits

- **~80% code sharing** between Android and iOS
- **Native performance** on both platforms
- **Single codebase** for business logic
- **Type-safe navigation** with state preservation
- **Reactive UI** with StateFlow + Compose

---

## Module Structure

```
project-root/
├── composeApp/              # UI layer (Android + iOS apps)
│   ├── src/
│   │   ├── androidMain/     # Android-specific code
│   │   ├── iosMain/          # iOS-specific code
│   │   ├── commonMain/       # Shared UI code
│   │   └── commonTest/       # Shared tests
│   └── build.gradle.kts
├── shared/                  # Business logic (KMP shared module)
│   ├── src/
│   │   ├── androidMain/       # Android-specific implementations
│   │   ├── iosMain/          # iOS-specific implementations
│   │   ├── jvmMain/          # JVM-specific implementations
│   │   ├── commonMain/       # Shared business logic
│   │   └── commonTest/       # Shared tests
│   └── build.gradle.kts
├── server/                  # Optional: Ktor backend
├── iosApp/                  # iOS Xcode project configuration
├── gradle/
│   └── libs.versions.toml   # Centralized dependency management
├── settings.gradle.kts
└── build.gradle.kts
```

---

## Root Project Configuration

### `settings.gradle.kts`

Defines project structure, repositories, and included modules:

```kotlin
rootProject.name = "yourapp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":server")      // Optional
include(":shared")
```

### `build.gradle.kts` (Root)

Applies plugins at the root level without configuration:

```kotlin
plugins {
    // Applied at root but configured in subprojects
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
}
```

---

## Version Catalog

Create `gradle/libs.versions.toml` for centralized dependency management:

```toml
[versions]
agp = "8.11.2"
android-compileSdk = "36"
android-minSdk = "24"
android-targetSdk = "36"
androidx-activity = "1.11.0"
androidx-lifecycle = "2.9.5"
composeMultiplatform = "1.9.1"
kotlin = "2.2.20"
ktor = "3.3.1"
decompose = "3.3.0"
koin = "4.0.4"
kotlinxSerialization = "1.8.1"
kotlinxCoroutines = "1.10.2"

[libraries]
# Kotlin
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }

# AndroidX
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
androidx-lifecycle-viewmodelCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
androidx-lifecycle-runtimeCompose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }

# Decompose (Navigation)
decompose-core = { module = "com.arkivanov.decompose:decompose", version.ref = "decompose" }
decompose-composeExtensions = { module = "com.arkivanov.decompose:extensions-compose", version.ref = "decompose" }

# Koin (Dependency Injection)
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-composeViewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

# Kotlinx
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }

# Ktor (Server - Optional)
ktor-serverCore = { module = "io.ktor:ktor-server-core-jvm", version.ref = "ktor" }
ktor-serverNetty = { module = "io.ktor:ktor-server-netty-jvm", version.ref = "ktor" }
logback = { module = "ch.qos.logback:logback-classic", version = "1.5.20" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinJvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
ktor = { id = "io.ktor.plugin", version.ref = "ktor" }
kotlinxSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

---

## Shared Module Setup

The **shared** module contains business logic that compiles to multiple platforms.

### `shared/build.gradle.kts`

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // iOS targets (ARM64 for devices, Simulator for emulator)
    iosArm64()
    iosSimulatorArm64()

    // JVM target (for server/desktop if needed)
    jvm()

    sourceSets {
        commonMain.dependencies {
            // Decompose for navigation logic
            implementation(libs.decompose.core)

            // Koin for DI
            implementation(libs.koin.core)

            // Kotlinx libraries
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.yourapp.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
```

### Shared Module Source Sets

```
shared/src/
├── commonMain/kotlin/        # Code shared across ALL platforms
│   ├── data/
│   │   ├── network/          # API service interfaces
│   │   ├── repository/       # Repository implementations
│   │   └── auth/             # Platform auth provider interfaces
│   ├── domain/
│   │   ├── usecases/         # Business logic use cases
│   │   └── validation/       # Input validation
│   ├── presentation/
│   │   ├── components/       # Decompose components (ViewModels)
│   │   └── state/            # UI state classes
│   └── di/
│       └── AppModule.kt      # Koin configuration
├── androidMain/kotlin/       # Android-specific implementations
│   └── di/
│       └── PlatformModule.android.kt
├── iosMain/kotlin/           # iOS-specific implementations
│   └── di/
│       └── PlatformModule.ios.kt
├── jvmMain/kotlin/           # JVM-specific implementations
│   └── di/
│       └── PlatformModule.jvm.kt
└── commonTest/kotlin/        # Shared tests
```

---

## ComposeApp Module Setup

The **composeApp** module contains the UI layer and platform-specific entry points.

### `composeApp/build.gradle.kts`

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // iOS framework configuration
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
        }

        commonMain.dependencies {
            // Compose BOM (Bill of Materials)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Reference to shared module
            implementation(projects.shared)

            // Decompose + Compose extensions
            implementation(libs.decompose.core)
            implementation(libs.decompose.composeExtensions)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewmodel)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.yourapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.yourapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
```

---

## Architecture Overview

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────┐
│                    PRESENTATION                       │
│  ┌─────────────────────────────────────────────────┐  │
│  │  UI Layer (Compose)                             │  │
│  │  - Screens                                       │  │
│  │  - Components                                    │  │
│  │  - Theme                                         │  │
│  └─────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────┤
│                    DOMAIN                             │
│  ┌─────────────────────────────────────────────────┐  │
│  │  Use Cases                                       │  │
│  │  - Business logic                                │  │
│  │  - Validation                                    │  │
│  └─────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────┤
│                      DATA                             │
│  ┌─────────────────────────────────────────────────┐  │
│  │  Repositories                                    │  │
│  │  - Data access abstraction                       │  │
│  └─────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────┤
│                   NETWORK/LOCAL                       │
│  ┌─────────────────────────────────────────────────┐  │
│  │  API Service / Database                          │  │
│  │  - Ktor client / SQLDelight                      │  │
│  └─────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

### Navigation with Decompose

Decompose provides type-safe, component-based navigation with full state preservation.

#### Root Component (Navigation Controller)

```kotlin
// shared/src/commonMain/kotlin/com/yourapp/presentation/components/RootComponent.kt

class RootComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()

    val childStack: Value<ChildStack<Config, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Splash,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Splash -> Child.Splash(
                SplashComponent(
                    componentContext = componentContext,
                    onNavigateToMain = { navigation.replaceAll(Config.Main) }
                )
            )
            is Config.Main -> Child.Main(
                MainComponent(
                    componentContext = componentContext,
                    onNavigateToDetail = { id ->
                        navigation.pushNew(Config.Detail(id))
                    }
                )
            )
            is Config.Detail -> Child.Detail(
                DetailComponent(
                    componentContext = componentContext,
                    itemId = config.id,
                    onNavigateBack = { navigation.pop() }
                )
            )
        }
    }

    // Serializable navigation configurations
    @Serializable
    sealed class Config {
        @Serializable
        data object Splash : Config()

        @Serializable
        data object Main : Config()

        @Serializable
        data class Detail(val id: String) : Config()
    }

    // Child wrapper classes
    sealed class Child {
        data class Splash(val component: SplashComponent) : Child()
        data class Main(val component: MainComponent) : Child()
        data class Detail(val component: DetailComponent) : Child()
    }
}
```

#### UI Integration

```kotlin
// composeApp/src/commonMain/kotlin/com/yourapp/App.kt

@Composable
fun App(rootComponent: RootComponent) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val childStack by rootComponent.childStack.subscribeAsState()

            Children(
                stack = childStack,
                animation = stackAnimation(fade() + scale())
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Splash -> SplashScreen(instance.component)
                    is RootComponent.Child.Main -> MainScreen(instance.component)
                    is RootComponent.Child.Detail -> DetailScreen(instance.component)
                }
            }
        }
    }
}
```

### Dependency Injection with Koin

#### Common Module (Shared)

```kotlin
// shared/src/commonMain/kotlin/com/yourapp/di/AppModule.kt

expect fun platformModule(): Module

val appModule = module {
    // Include platform-specific module
    includes(platformModule())

    // ==================== NETWORK ====================
    single<ApiService> { KtorApiService() }

    // ==================== REPOSITORIES ====================
    single { UserRepository(get()) }
    single { AuthRepository(get()) }

    // ==================== USE CASES ====================
    factory { SignInUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }

    // ==================== COMPONENTS ====================
    factory { RootComponentFactory() }
}
```

#### Platform-Specific Modules

```kotlin
// shared/src/androidMain/kotlin/com/yourapp/di/PlatformModule.android.kt

actual fun platformModule(): Module = module {
    single<GoogleAuthProvider> { AndroidGoogleAuthProvider(get()) }
}
```

```kotlin
// shared/src/iosMain/kotlin/com/yourapp/di/PlatformModule.ios.kt

actual fun platformModule(): Module = module {
    single<GoogleAuthProvider> { IOSGoogleAuthProvider() }
}
```

---

## Platform Entry Points

### Android Entry Point

#### MainActivity.kt

```kotlin
// composeApp/src/androidMain/kotlin/com/yourapp/MainActivity.kt

package com.yourapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import com.yourapp.presentation.components.RootComponent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // retainedComponent keeps RootComponent alive across config changes
        val rootComponent = retainedComponent { componentContext ->
            RootComponent(componentContext)
        }

        enableEdgeToEdge()

        setContent {
            App(rootComponent)
        }
    }
}
```

#### Application Class (Koin Initialization)

```kotlin
// composeApp/src/androidMain/kotlin/com/yourapp/YourApp.kt

package com.yourapp

import android.app.Application
import com.yourapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class YourApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@YourApp)
            modules(appModule)
        }
    }
}
```

#### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".YourApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:exported="true"
            android:name=".MainActivity"
            android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|density">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### iOS Entry Point

#### Kotlin/Native ViewController

```kotlin
// composeApp/src/iosMain/kotlin/com/yourapp/MainViewController.kt

package com.yourapp

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.yourapp.di.appModule
import com.yourapp.presentation.components.RootComponent
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    val lifecycle = LifecycleRegistry()
    val componentContext = DefaultComponentContext(lifecycle = lifecycle)
    val rootComponent = RootComponent(componentContext)

    App(rootComponent)
}

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}
```

#### Swift Integration (iOS App)

```swift
// iosApp/iosApp/iOSApp.swift

import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Initialize Koin dependency injection
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// iosApp/iosApp/ContentView.swift

import SwiftUI
import ComposeApp

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

---

## Building and Running

### Gradle Commands

```bash
# Build Android APK (Debug)
./gradlew :composeApp:assembleDebug

# Install and run on connected device
./gradlew :composeApp:installDebug

# Run unit tests (shared module)
./gradlew :shared:test

# Run Android tests
./gradlew :composeApp:testDebugUnitTest

# Build iOS framework (for Xcode)
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode

# Clean build
./gradlew clean

# Build all modules
./gradlew build
```

### IDE Setup

#### Android Studio
1. Install the **Kotlin Multiplatform Mobile** plugin
2. Open the project root folder
3. Sync Gradle files
4. Select Android device and run

#### Xcode (iOS)
1. Run `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`
2. Open `iosApp/iosApp.xcodeproj`
3. Build and run on iOS Simulator or device

---

## Technology Stack Summary

| Category | Technology | Purpose |
|----------|-----------|---------|
| **Language** | Kotlin 2.2+ | Primary development language |
| **UI Framework** | Compose Multiplatform 1.9+ | Shared UI across platforms |
| **Navigation** | Decompose 3.3+ | Type-safe component-based routing |
| **Dependency Injection** | Koin 4.0+ | Service location and DI |
| **Networking** | Ktor Client 3.3+ | HTTP client for API calls |
| **Serialization** | Kotlinx Serialization 1.8+ | JSON parsing/generation |
| **Coroutines** | Kotlinx Coroutines 1.10+ | Async programming |
| **Build System** | Gradle 8.0+ with Version Catalogs | Dependency management |
| **Android** | API 24+ (minSdk), API 36 (target) | Android platform support |
| **iOS** | iOS 14+ | iOS platform support |

---

## Recommended Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
- [Decompose Navigation](https://arkivanov.github.io/Decompose/)
- [Koin DI Framework](https://insert-koin.io/)
- [Ktor Client](https://ktor.io/docs/client.html)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

---

## License

This guide is provided as-is for educational purposes. Adapt the configuration to your specific project requirements.
