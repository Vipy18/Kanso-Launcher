# Kanso Launcher

A Tactile Cyber-Mecha HUD Android launcher built with Kotlin and Jetpack Compose, designed for **Global AOSP** and **Chinese OEM ROMs** (including **Honor MagicOS 9.0 / Android 15**, Huawei HarmonyOS/EMUI, Xiaomi HyperOS/MIUI, OPPO ColorOS, and Vivo OriginOS).

---

## Features

- **Modular Home Workspace & Multi-Theme Widget Matrix**: Snap-to-grid workspace supporting multiple pages, live multi-theme Kanso widgets (Clock, Battery Capsule, Network/RAM/CPU Telemetry Pods, Tri-State Ringer, Dual Luminance/Volume Sliders, 4×2 & 2×2 Action Matrices, Media HUD, Open-Meteo Weather Badge, 7-Day Calendar Ribbon, Accent Blocks), folders, icon pack overrides, and third-party Android AppWidgets.
- **Split Notification & Control Shade**:
  - **Left swipe-down**: Notification Panel with amber luminance canopy, quick toggle strip, live Media HUD deck, and color-coded active notifications.
  - **Right swipe-down**: Control Panel with audio bus slider, audio output router, and customizable 3-column Hardware Matrix with Edit Mode.
- **3D Depth-Stack Multitasking (Recents)**:
  - Swipe-up card dismiss, card **X** close, and **CLEAR** unpinned tasks terminate background processes via `ActivityManager.killBackgroundProcesses`, evict cached window snapshots, and persist dismissal cutoffs so dismissed apps stay cleared until relaunched.
  - Long-press the card **X** button (or tap the dedicated Force Stop icon) to open Android App Info for terminating persistent foreground services.
  - Tap any blank space around or between cards to return directly to Kanso Home without surfacing the OEM stock launcher.
- **3-Tier Battery Saver Toggle**:
  1. **Tier 1 (`WRITE_SECURE_SETTINGS`)**: Directly toggles `Settings.Global["low_power"]` (plus Honor/Huawei `Settings.System["SmartModeStatus"]`) and verifies `PowerManager.isPowerSaveMode` after 300ms.
  2. **Tier 2 (Accessibility Quick Settings)**: Matches locale-aware Quick Settings tiles (`Battery Saver`, `Power saving`, `省电模式`, `低电量模式`, `节电模式`) and dismisses the shade automatically.
  3. **Tier 3 (Settings Fallback)**: Opens `Settings.ACTION_BATTERY_SAVER_SETTINGS` only if both tiers fail.
- **3-State Sound Mode Cycling**: Clean `Ring → Vibrate → Silent → Ring` cycling with Do Not Disturb policy checks, OEM broadcast race guards, and automatic ring-volume restoration.
- **App Drawer, Discover Feed, Lockscreen & Always-On Display**: Recessed tactile app drawer with icon pack support, RSS/Atom Discover feed with bookmarks, optional Lockscreen Companion, AMOLED `DreamService` AOD with pixel-shift burn-in protection, and a 60FPS `Choreographer`-driven Performance Governor.

---

## Architecture & Tech Stack

| Layer | Technologies & Components |
| :--- | :--- |
| **UI & Styling** | Jetpack Compose (Material 3), custom neumorphic + cyber-mecha modifiers, edge-to-edge `WindowInsets`, Space Grotesk / JetBrains Mono / Space Mono / Geist typography |
| **State & Architecture** | MVVM (`LauncherViewModel` + `StateFlow` / `collectAsStateWithLifecycle`) |
| **Persistence** | Room (`KansoDatabase`, `KansoDao` for workspace items, folders, dock, hidden apps, icon overrides), Jetpack DataStore (`KansoPreferences`), `SharedPreferences` (`RecentsSnapshotCache` dismissal cutoffs) |
| **System Services** | `KansoAccessibilityService` (OEM Home/Recents override, redirect suppression, Quick Settings tile fallback, Android 11+ window snapshots), `KansoNotificationListener` (notifications & live media sessions), `KansoLifecycleService` + `KansoPerformanceGovernor`, `KansoDreamService` (AMOLED AOD) |
| **Telemetry & Data** | `TelemetryRepository` (battery, network speed, RAM/storage, audio/ringer, radios, Battery Saver), `WeatherRepository` (Open-Meteo), `DiscoverFeedRepository` (RSS/Atom), `IconPackManager`, `AudioVisualizerEngine` |
| **Testing** | JUnit 4, Robolectric 4.15.1 (SDK 35), Compose UI Test (`createComposeRule`), Kotlin Coroutines Test |

- **Package ID**: `com.kanso.launcher`
- **SDK Targets**: `minSdk = 26`, `targetSdk = 35`, `compileSdk = 36`, Java 21

---

## Building & Testing

### Prerequisites
- JDK 21
- Android SDK Platform 36 & Build Tools installed (`ANDROID_SDK_ROOT` configured)

### Build Commands

```bash
# Build Debug APK
./gradlew :app:assembleDebug

# Build Release APK (outputs app/build/outputs/apk/release/app-release.apk)
./gradlew :app:assembleRelease

# Run full JVM, Robolectric, and Compose UI test suite (45+ tests)
./gradlew :app:testDebugUnitTest

# Run Android Lint
./gradlew :app:lintDebug
```

> **Note on Signing**: Configure a production release keystore in `app/build.gradle.kts` before publishing to end users outside personal testing.

---

## Device Setup & Chinese OEM / Honor MagicOS 9.0 Guide

Kanso Launcher works out-of-the-box on Global AOSP and includes dedicated workarounds for **Honor MagicOS 9.0 (Android 15, e.g. `LGE-AN10`)**, Huawei HarmonyOS/EMUI, Xiaomi HyperOS/MIUI, OPPO ColorOS, and Vivo OriginOS.

### 1. Required ADB Grants (Re-run After Clean Installs)

Android resets `WRITE_SECURE_SETTINGS` and `WRITE_SETTINGS` app-ops on every clean install. After installing or reinstalling the APK, run:

```bash
adb shell pm grant com.kanso.launcher android.permission.WRITE_SECURE_SETTINGS
adb shell appops set com.kanso.launcher WRITE_SETTINGS allow
```

### 2. In-App & System Permissions

Open **Kanso Settings → System Health** to grant:
- **Default Home App**: Set Kanso Launcher as the default Home launcher.
- **Usage Access (`PACKAGE_USAGE_STATS`)**: Required for the 3D Multitasking / Recents deck.
- **Accessibility Service (`Kanso Accessibility Bridge`)**: Enables Recents override, Chinese OEM Home redirection, screenshot capture, assisted Force Stop, and Tier 2 Quick Settings Battery Saver toggling.
- **Notification Access (`Kanso Notification & Media Bridge`)**: Powers the split Notification Panel, app badge dots, and live Media HUD deck.
- **Do Not Disturb Policy Access**: Required on Android 7+ and Honor MagicOS to switch directly into Silent mode without triggering OEM audio fallback issues.
- **Background & Auto-Launch (Chinese OEMs)**: Set Kanso Launcher to **Manual / Unrestricted** battery management (Auto-launch, Secondary launch, Run in background) in Honor/Huawei App Launch settings so MagicOS does not kill the Accessibility Service.

### 3. On-Device Verification Commands

```bash
# 1. Verify Battery Saver toggle flips global low_power without opening Settings
adb shell settings get global low_power

# 2. Verify dismissed Recents app background process was terminated
adb shell ps -A | grep <package_name>

# 3. Verify tapping empty Recents space returns directly to Kanso LauncherActivity
adb shell dumpsys activity activities | grep mResumedActivity
```