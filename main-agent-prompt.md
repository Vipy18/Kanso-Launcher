<role_and_orchestration>
You are a Principal Android Systems Architect acting as the **Main Orchestrator Agent** building **Kanso-Launcher** (`com.kanso.launcher`) — a complete, compilable Android Home Screen Launcher in **Kotlin**, **Jetpack Compose**, and **Material 3**.

STRICT OPERATING RULES:
1. **Discover & Use Plugins First:** Before writing code, inspect all available workspace plugins and MCP tools (especially **Google Stitch MCP**, file/codebase tools, and **Gradle/Android Lint build tools**). Use Stitch MCP to generate/validate UI screens and extract design tokens, and use build/lint plugins to verify compilation. If a plugin is not connected in your current environment, state that briefly and proceed by generating exact Stitch prompts and clean manual verification steps.
2. **Deploy Subagents With Strict File Ownership:** Delegate work across the 5 specialized subagents defined in `<subagent_plan>`. Every subagent must obey `<visual_source_of_truth>` and `<android_api_guardrails>`. Merge all outputs into one unified, compiling project with ZERO `// TODO`, `...`, or placeholder comments.
3. **Zero Missing Assets Rule:** Do NOT reference external `.ttf` font files or `.png` files that you do not create. Render the dot-matrix clock using a pure-Kotlin Compose `Canvas` 5×7 pixel-matrix renderer, render neumorphic shadows using standard Compose `drawBehind` + Android framework `Paint.setShadowLayer`, and render all built-in icons using `Icons.Rounded.*` or custom Kotlin `ImageVector` / `Canvas` code.
</role_and_orchestration>

<visual_source_of_truth>
Inspect all 4 attached reference images carefully before doing any work. Whenever two images disagree, obey this exact conflict-resolution hierarchy:

1. **Image 1 — Light Home Screen Mockup (PRIMARY LAYOUT TRUTH):**
   - Defines the exact Home Screen grid, widget proportions, S-curve top color-block, Light Mode matte off-white surface, and bottom 4-icon floating dark dock.
   - **Rule:** The Home Screen does **NOT** show a search bar by default (keep the Home Screen clean like Image 1; search lives in the App Drawer, with an optional toggle in Settings).
2. **Image 2 — Annotated UI Specification (CONCEPTUAL LABELS ONLY):**
   - Use ONLY to understand component names and roles.
   - **Ignore artifacts:** Ignore the garbled text at the bottom-left and ignore the music-note icon swap (use the **Play + Wi-Fi** dual pill from Image 1, or let the user tap to configure toggle actions).
3. **Image 3 — Dark Mode + App Drawer Mockup (DARK MODE & DRAWER TRUTH):**
   - Defines the exact Dark Mode matte carbon surfaces, dark bevel shadows, and accent-tinted iconography.
   - Defines the **App Drawer** peeking on the right: curved top accent zone containing a recessed dark `Search Apps...` pill + dot-matrix grid/list toggle icon, a live micro-telemetry strip (`XX Apps Installed` | `RAM: X.XGB Free`), tactile neumorphic app tiles, and a thin right-edge vertical `A–Z` scrubber rail.
   - **Full-Screen Drawer Rule:** Since the App Drawer is partially occluded in Image 3, render it full-width when opened—showing a **4-column icon grid** (default) switchable to a **1-column detailed list**, styled identically in both Light Mode (matte off-white canvas + dark text) and Dark Mode (matte carbon canvas + off-white text).
4. **Image 4 — App Icon Reference (ADAPTIVE ICON TRUTH):**
   - Replicate the curved accent-orange / matte-off-white split background, centered matte-black stadium pill switch with a glowing orange circular indicator dot on the left, subtle dot-matrix grid behind it, and 4 tiny directional micro-arrows around it.
   - **Ignore artifacts:** Do NOT draw the faint debossed phone receiver inside the right knob, and do NOT draw the small four-point star watermark in the bottom-right corner.
</visual_source_of_truth>

<product_overview>
- **App Name:** `Kanso-Launcher`
- **Package Name:** `com.kanso.launcher`
- **Design Identity:** One single unified aesthetic — **Mecha-Industrial Neumorph with Bold Color-Blocking**. Do NOT build multiple unrelated themes.
- **User Personalization (Built Into Settings):**
  1. **Theme Mode Toggle:** `Light` | `Dark` | `Follow System` (instant animated transition via `animateColorAsState`, persisted in Preferences DataStore).
  2. **Dynamic Accent Color:** Default **Signal Orange (`#FF4500`)**, plus curated industrial swatches (`#E62E2D` Crimson, `#F2B705` Hazard Amber, `#00B8D9` Mecha Cyan, `#84CC16` Acid Lime, `#2563EB` Cobalt Steel) AND a custom **HEX / HSV Color Picker**. Automatically compute high-contrast `onAccent` ink (`#111112` or `#FFFFFF`) using WCAG luminance math.
  3. **Custom Icon Packs:** User can choose between **(a) Kanso Built-In Icons** (monochrome/accent-tinted adaptive glyphs inside neumorphic wells), **(b) System Default Icons**, or **(c) Any Installed Third-Party Android Icon Pack** (parsed via `appfilter.xml` + fallback masking + per-app icon override).
  4. **Canvas Surface Option:** Solid Matte Hardware Surface (default, matching mockups) OR optional subtle dimmed System Wallpaper overlay behind the lower canvas.
</product_overview>

<stitch_and_design_system>
### Google Stitch Workflow (Subagent 1)
Before writing Compose UI code, generate `DESIGN.md` using Google Stitch (via Stitch MCP if available; otherwise author `DESIGN.md` plus exact Stitch generation prompts for all 5 screens: Home Light, Home Dark, App Drawer, Lockscreen/AOD, and Kanso Settings).

### Exact Design Tokens (Hardcode into `KansoTheme.kt`)
- **Accent Color (`accent`):** Dynamic from DataStore (Default `#FF4500`). Used for: Top S-curve color-block, active hardware toggles, active calendar/A–Z indicators, circular progress arcs, and optional accent icon tint.
- **Light Mode Tokens:**
  - `canvasBg`: `#EAECEF` (Matte Off-White)
  - `surfaceRaised`: `#F0F2F5`
  - `surfaceInset`: `#E2E5EA`
  - `shadowLight`: `#FFFFFF` (alpha 0.90f, blur `12.dp`, offset `(-5.dp, -5.dp)`)
  - `shadowDark`: `#C5CAD3` (alpha 0.85f, blur `14.dp`, offset `(6.dp, 6.dp)`)
  - `darkModuleBg`: `#1A1B1E` (for Dock, Volume capsule, Play/Wi-Fi pill)
  - `inkPrimary`: `#111112` | `inkSecondary`: `#6B7078`
- **Dark Mode Tokens:**
  - `canvasBg`: `#121214` (Matte Carbon AMOLED)
  - `surfaceRaised`: `#1C1D21`
  - `surfaceInset`: `#141518`
  - `shadowLight`: `#2C2E35` (alpha 0.60f, blur `8.dp`, offset `(-3.dp, -3.dp)`)
  - `shadowDark`: `#050506` (alpha 0.90f, blur `12.dp`, offset `(5.dp, 5.dp)`)
  - `darkModuleBg`: `#18191C`
  - `inkPrimary`: `#F2F3F5` | `inkSecondary`: `#8E929C`

### Exact Custom Geometry & Modifiers
1. **`Modifier.kansoNeumorphic(raised: Boolean, cornerRadius: Dp)`:**
   - Implement using Compose `drawBehind` and native `android.graphics.Paint` with `setShadowLayer` (ensuring `View.LAYER_TYPE_SOFTWARE` or Android 9+ hardware-accelerated shadow compatibility so it never crashes).
   - Pressing any interactive card triggers a spring scale (`0.96f`), haptic tick (`HapticFeedbackType.TextHandleMove`), and flattens shadow depth.
2. **Curved Top Color-Block (`ColorBlockHeaderShape`):**
   - Custom Compose `Canvas` `Path` covering the top ~34% of the screen width-to-width, extending lower on the left side (~37% screen height) to frame the Hero Clock + Date, then sweeping smoothly upward via a cubic Bézier S-curve to ~26% height on the right side to hug the dark Volume/Status capsule.
3. **Dot-Matrix Clock Renderer (`DotMatrixClock`):**
   - Pure Kotlin 5×7 boolean matrix map for digits `0`–`9`. Draws crisp square pixels with uniform spacing. Stacks `HH` above `MM` on the left, separated by two tiny centered 5-dot diamond/cross (`+ +`) glyphs.
</stitch_and_design_system>

<screen_by_screen_requirements>
### Screen 1: Home Screen (`HomeScreen.kt`)
- **Top Accent Color-Block Zone:**
  - Left: `DotMatrixClock` (`21` stacked over `03`) + condensed date (`Today Monday, Aug 18`). Tapping opens the system Clock/Alarm app.
  - Right: Translucent dark status capsule (`#1A1B1E`) with a status dot on the left and a tactile circular accent-colored Volume/Sound toggle button on the right (toggles Ring / Vibrate / Silent or opens volume HUD via `AudioManager`).
- **Modular Center Workspace (Snap-to-Grid Canvas, Multi-Page HorizontalPager):**
  - Default Page 1 reproduces the exact layout of Image 1 with a vertical micro dot-scale accent along the left edge.
  - Supports **Long-Press Edit Mode**: drag-and-drop to move widgets/apps/folders, resize widgets, add/remove built-in widgets or external Android `AppWidgetHost` widgets, and a quick button to open **Kanso Settings**.
  - Supports **Circular App Folders**: grouping apps into a single circular cluster tile (showing a 2×2 mini-grid of icons inside a dark/neumorphic circle) that expands into a clean neumorphic folder dialog on tap.
  - Displays subtle **Notification Dots** on app icons (driven by `NotificationListenerService` when granted).
- **Floating Mecha Dock (Bottom):**
  - Fixed dark capsule (`#1A1B1E`, height `72.dp`, corner radius `36.dp`) floating `16.dp` above the navigation bar insets.
  - Holds 4 (configurable up to 5) circular recessed app slots (defaults: Phone, Messages, Browser, Camera). Respects the active Icon Pack or Kanso Monochrome style.
- **Home Gestures:**
  - Swipe Up anywhere -> Smooth spring transition into **App Drawer**.
  - Swipe Down anywhere -> Expand Android Notification Shade (via reflection fallback or `AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS`).
  - Double-Tap empty canvas -> Lock screen (via `AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN`, prompting user cleanly to enable if not yet granted).
  - Swipe Left/Right -> Navigate Home Pages.

### Screen 2: Built-In Modular Widget Library (All 7 Custom Compose Widgets + `AppWidgetHost`)
Every widget must react to live system state and match Image 1 & Image 3:
1. **Dot-Matrix Hero Clock & Date:** Live 12h/24h ticker via `ACTION_TIME_TICK`.
2. **Recessed Battery Capsule:** Horizontal stadium pill (`inset` neumorphic well). Left 35% renders a circular dot-matrix halftone pattern scaled to battery level; right 65% shows live `XX% Battery` (plus lightning bolt when charging) via `BroadcastReceiver(ACTION_BATTERY_CHANGED)`.
3. **Diagonal Network Telemetry Card:** Custom angled stadium card (`rotate(-45f)` container with upright inner content) displaying live `X.XX KB/s` (computed every 1.5s from `TrafficStats.getTotalRxBytes() + getTotalTxBytes()`) and a bottom-left circular progress gauge ring.
4. **Hardware Quick-Toggle Cluster:**
   - **Dual Dark Pill:** Left = Media Play/Pause toggle (accent icon); Right = Wi-Fi button (accent icon).
   - **Twin Accent Circles (`52.dp`):** Left = Mobile Data (`↑↓`); Right = Bluetooth (`ᛒ`).
   - *(See `<android_api_guardrails>` for exact Android 10–15 safe toggle behavior!)*
5. **Media HUD Card:** Raised neumorphic rounded rectangle (`20.dp` radius) bound to `NotificationListenerService` + `MediaSessionManager`. Shows rounded-square album art thumbnail, track title + artist (fallback: `"Media HUD"` / `"Tap to play"`), and two tiny circular neumorphic Previous/Next buttons (tap art or center to Play/Pause).
6. **Scalloped Weather Badge:** Custom 8-lobe wavy/scalloped `GenericShape` (`Canvas` math) displaying `Weather 20°`, minimalist weather vector icon (umbrella/sun/cloud), and micro-label `HUMIDITY 86%`. Fetch live weather via free **Open-Meteo API** (no API key needed; use coarse location if granted or manual city fallback, cached in DataStore, with graceful offline mock fallback).
7. **7-Day Calendar Ribbon:** Full-width raised neumorphic pill showing `SUN MON TUE WED THU FRI SAT` over date numbers, highlighting today's date inside a filled carbon/accent circle. Tapping opens the system Calendar app.
8. **External Android Widgets:** Include a clean `AppWidgetHost` + `AppWidgetManager` picker & `AndroidView` wrapper so users can also drop any third-party app widget onto secondary pages or swap out built-in slots.

### Screen 3: App Drawer (`AppDrawerScreen.kt`)
- **Top Curved Accent Header:** Matching S-curve in the active Accent Color holding a recessed dark pill search field (`Search Apps...`) + right-side dot-matrix toggle button (switches between 4-column Grid and 1-column List) + Settings gear icon.
- **Live System Telemetry Strip:** Directly below the curve: tiny dot-matrix icon + `XX Apps Installed` and live `RAM: X.XGB Free` (queried from `ActivityManager.getMemoryInfo`).
- **App List / Grid:** Smooth `LazyVerticalGrid` / `LazyColumn` of all launchable apps (excluding user-hidden apps) inside tactile neumorphic wells.
  - **Long-Press Context Sheet on Any App:** `Add to Home`, `Pin to Dock`, `Change Icon (Per-App Override)`, `Hide App`, `App Info`, `Uninstall`.
- **Vertical A–Z Scrubber Rail:** Thin right-edge strip (`A`..`Z`, `#`) that smoothly scrolls the list on drag, highlights the active letter in the Accent Color, displays a floating accent letter bubble, and triggers subtle haptic ticks.

### Screen 4: Lockscreen & Always-On Display (`Lockscreen` & `KansoDreamService`)
- **Strict Non-Root Android Architecture (Do NOT hallucinate broken system-keyguard replacement):**
  1. **Always-On Display (`KansoDreamService : DreamService`):** Native Android screensaver selectable in Android Display Settings. Pure `#000000` AMOLED canvas with subtle burn-in pixel shift every 60 seconds, displaying the oversized Dot-Matrix Clock, date, circular battery ring, minimalist notification dots, and subtle accent curve.
  2. **Optional Lockscreen Companion (`LockscreenActivity`):** Uses `setShowWhenLocked(true)` + `setTurnScreenOn(true)` triggered optionally via a lightweight foreground/screen-on receiver when enabled in Settings. Displays pure AMOLED black + minimal accent curve, Dot-Matrix Clock, compact Media HUD bar (when playing), notification icon pills, and a tactile bottom swipe-up unlock pill that calls `KeyguardManager.requestDismissKeyguard()` / `BiometricPrompt`.

### Screen 5: Kanso Settings & Permissions Center (`SettingsScreen.kt`)
Accessible via Home Edit Mode or App Drawer header, styled in the exact same neumorphic hardware UI:
- **Appearance:** Segmented pill toggle (`Light` | `Dark` | `System`) + `Solid Matte` vs `Subtle Wallpaper` switch.
- **Accent Color:** 6 industrial preset swatches + interactive HSV/HEX Custom Color Picker with live S-curve mini-preview.
- **Icon Pack Manager:** Select `Kanso Built-In (Monochrome)`, `Kanso Built-In (Accent)`, `System Default`, or any detected installed third-party Icon Pack (showing live 4-icon preview per pack).
- **Drawer & Home Options:** Show/Hide Home Search Bar, Dock slot count (4 or 5), Manage Hidden Apps, Set as Default Home Launcher shortcut (`Settings.ACTION_HOME_SETTINGS`).
- **Permissions Health Center:** Live green/orange status badges + one-tap grant buttons for `Notification Access` (Media HUD + Badges), `Accessibility Service` (Double-tap Lock + Swipe-down Shade), `Bluetooth Connect` (Android 12+), and `Location` (Weather).
</screen_by_screen_requirements>

<android_api_guardrails>
OBEY EVERY ITEM BELOW — DO NOT USE DEPRECATED OR SILENTLY-FAILING ANDROID APIS:
1. **Single-Module Architecture (Compile Reliability):** Use ONE `:app` module organized cleanly by package (`ui.theme`, `ui.home`, `ui.drawer`, `ui.widgets`, `ui.lockscreen`, `ui.settings`, `data.apps`, `data.iconpacks`, `data.telemetry`, `data.weather`, `data.prefs`, `service`). Do NOT split into 8 Gradle modules.
2. **Android 11+ App & Icon Pack Visibility (`AndroidManifest.xml`):**
   - MUST declare `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />` AND an explicit `<queries>` block containing:
     - `<intent><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent>`
     - `<intent><action android:name="org.adw.launcher.THEMES" /></intent>`
     - `<intent><action android:name="com.novalauncher.THEME" /></intent>`
     - `<intent><action android:name="com.anddoes.launcher.THEME" /></intent>`
     - `<intent><action android:name="com.gau.go.launcherex.theme" /></intent>`
3. **Default Home Intent Filter:** `LauncherActivity` MUST have `launchMode="singleTask"`, `clearTaskOnLaunch="true"`, `stateNotNeeded="true"`, and categories `HOME`, `DEFAULT`, and `LAUNCHER`.
4. **Live App Sync:** Register `LauncherApps.Callback` in `AppRepository` so installing, updating, or uninstalling an app updates the App Drawer and Home Screen immediately without restarting.
5. **Android 10–15 Safe Hardware Toggles:**
   - **NEVER** call `WifiManager.setWifiEnabled()` (blocked on Android 10+). Tapping the Wi-Fi or Mobile Data button MUST launch `Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)` on Android 10+ (fallback to `Settings.ACTION_WIFI_SETTINGS` / `Settings.ACTION_DATA_ROAMING_SETTINGS`).
   - **Bluetooth:** Check `Manifest.permission.BLUETOOTH_CONNECT` on Android 12+ (`API >= 31`). Since `BluetoothAdapter.enable()/disable()` is deprecated on API 33+, open `Settings.ACTION_BLUETOOTH_SETTINGS` or request `BluetoothAdapter.ACTION_REQUEST_ENABLE` cleanly without crashing.
   - **Volume:** Use `AudioManager.adjustVolume()` / ringer mode check with `NotificationManager.isNotificationPolicyAccessGranted` guard before switching to Silent/DND.
6. **Custom Icon Pack Parsing (`IconPackManager.kt`):**
   - Load target icon pack resources via `packageManager.getResourcesForApplication(packPackage)`.
   - Parse `res/xml/appfilter.xml` (fallback to `assets/appfilter.xml`) with `XmlPullParser` on `Dispatchers.IO`.
   - Support `<iconback>`, `<iconmask>`, `<iconupon>`, and `<scale>` compositing using `Canvas` + `PorterDuffXfermode(PorterDuff.Mode.DST_OUT)` for unthemed apps.
   - Cache resulting `ImageBitmap`s in an `LruCache` so scrolling stays at 60–120fps.
7. **Target & Build Spec:**
   - `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, Kotlin 2.0+, Jetpack Compose BOM, Material 3, Room (with KSP), Preferences DataStore, Coroutines/StateFlow, `kotlinx.serialization`, and `HttpURLConnection` (or lightweight Ktor/OkHttp) for Open-Meteo.
</android_api_guardrails>

<subagent_plan>
Coordinate these 5 subagents sequentially/in parallel with non-overlapping file ownership:
- **Subagent 1 (Stitch & Design System):** Produces `DESIGN.md`, `ui/theme/*` (`KansoTheme.kt`, `NeumorphicModifier.kt`, `ColorBlockHeader.kt`, `DotMatrixClock.kt`, `ScallopedShape.kt`), and Adaptive Icon XML drawables (`res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`).
- **Subagent 2 (App Core & Icon Pack Engine):** Produces `AndroidManifest.xml`, `data/apps/AppRepository.kt` (with `LauncherApps.Callback`), `data/iconpacks/IconPackManager.kt` (`appfilter.xml` + masking + LRU cache), `data/prefs/KansoPreferences.kt` (DataStore), and Room DB (`data/db/*` for grid positions, folders, hidden apps, per-app icon overrides).
- **Subagent 3 (Telemetry, Media & Lockscreen Services):** Produces `data/telemetry/TelemetryRepository.kt` (Battery, `TrafficStats` KB/s, RAM, Audio/Network status), `data/weather/WeatherRepository.kt` (Open-Meteo), `service/KansoNotificationListener.kt` (MediaSession + Notification badges), `service/KansoAccessibilityService.kt` (Lock + Notification Shade), `service/KansoDreamService.kt` (AOD), and `ui/lockscreen/LockscreenActivity.kt`.
- **Subagent 4 (Widgets, Home Screen, App Drawer & Settings UI):** Produces all 7 built-in widgets (`ui/widgets/*`), `AppWidgetHostWrapper.kt`, `ui/home/HomeScreen.kt` (with Edit Mode, Circular Folders, Floating Dock, Gestures), `ui/drawer/AppDrawerScreen.kt` (Grid/List, Search, Telemetry, A–Z Rail), and `ui/settings/SettingsScreen.kt`.
- **Subagent 5 (Integration, Gradle & Verification):** Produces `build.gradle.kts`, `settings.gradle.kts`, `LauncherActivity.kt`, `KansoApplication.kt`, wires all ViewModels/DI cleanly, and runs Gradle/Lint verification via available plugins to confirm zero compile errors.
</subagent_plan>

<output_protocol>
Execute in this exact order:
1. **Plugin & Subagent Kickoff:** List detected plugins/MCP tools (including Google Stitch & Gradle tools) and confirm the 5 subagents.
2. **Step 1 — `DESIGN.md`:** Output the full Stitch design specification, token tables, and custom geometry math.
3. **Step 2 — Complete Project Tree:** Show every file path in `com.kanso.launcher`.
4. **Step 3 — Complete Compilable Source Code:** Output every file in full with its exact relative path header. NEVER abbreviate functions or leave `// TODO` stubs.
5. **Pagination Rule:** If you approach your response length limit, stop cleanly right after finishing a complete file, list which files remain, and tell me to type `"continue"`.
</output_protocol>
