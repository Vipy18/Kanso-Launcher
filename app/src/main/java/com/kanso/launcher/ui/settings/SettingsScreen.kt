package com.kanso.launcher.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import com.kanso.launcher.R
import com.kanso.launcher.data.iconpacks.IconPackInfo
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.lockscreen.LockscreenActivity
import com.kanso.launcher.ui.recents.RecentsActivity
import com.kanso.launcher.ui.theme.ColorBlockHeaderBackdrop
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.util.RomDetector
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val colors = LocalKansoColors.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val health by viewModel.permissionsHealth.collectAsStateWithLifecycle()
    val governor by viewModel.governorState.collectAsStateWithLifecycle()
    val installedPacks by viewModel.installedIconPacks.collectAsStateWithLifecycle()
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val hiddenPackages by viewModel.hiddenPackages.collectAsStateWithLifecycle()
    val cityResults by viewModel.citySearchResults.collectAsStateWithLifecycle()

    var cityQuery by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) viewModel.enableAutoLocationWeather()
        else viewModel.refreshWeather(force = true)
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    val allRuntimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    val defaultHomeRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        viewModel.refreshIconPacks()
        onPauseOrDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvasBg)
    ) {
        ColorBlockHeaderBackdrop(
            height = 136.dp,
            leftDropFraction = 0.96f,
            rightShelfFraction = 0.74f,
            curveCenterXFraction = 0.60f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Settings Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .testTag("settings_back_button")
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_back),
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "KANSO SETTINGS",
                        color = colors.onAccent,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "HARDWARE UI & SYSTEM HEALTH",
                        color = colors.onAccent.copy(alpha = 0.78f),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // 1. APPEARANCE
                item {
                    SectionLabel("01 // APPEARANCE")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Launcher Theme (Independent of System)",
                                color = colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .kansoNeumorphic(raised = false, cornerRadius = 20.dp)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    val selected = settings.themeMode == mode
                                    val label = when (mode) {
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.SYSTEM -> "System"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .testTag("theme_mode_${mode.name.lowercase()}")
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (selected) colors.accent else Color.Transparent)
                                            .kansoTactileClickable { viewModel.setThemeMode(mode) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (selected) colors.onAccent else colors.inkPrimary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        SettingsToggleRow(
                            title = "Subtle Wallpaper Surface",
                            subtitle = if (settings.useWallpaperOverlay) {
                                "System wallpaper visible (${settings.wallpaperTransparencyPercent}%)"
                            } else {
                                "Solid matte hardware surface"
                            },
                            checked = settings.useWallpaperOverlay,
                            testTag = "toggle_wallpaper_overlay",
                            onCheckedChange = viewModel::setUseWallpaperOverlay
                        )

                        AnimatedVisibility(visible = settings.useWallpaperOverlay) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Wallpaper Transparency",
                                        color = colors.inkPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${settings.wallpaperTransparencyPercent}%",
                                        color = colors.accent,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Slider(
                                    value = settings.wallpaperTransparencyPercent.toFloat(),
                                    onValueChange = { viewModel.setWallpaperTransparencyPercent(it.roundToInt()) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.accent,
                                        activeTrackColor = colors.accent,
                                        inactiveTrackColor = colors.inkSecondary.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .testTag("slider_wallpaper_transparency")
                                        .fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "0% Solid",
                                        color = colors.inkSecondary,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp
                                    )
                                    Text(
                                        text = "100% Full Wallpaper",
                                        color = colors.inkSecondary,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp
                                    )
                                }
                            }
                        }

                        SettingsToggleRow(
                            title = "24-Hour Dot-Matrix Clock",
                            subtitle = "Switch between 24h (21:03) and 12h (09:03)",
                            checked = settings.use24HourClock,
                            testTag = "toggle_24h_clock",
                            onCheckedChange = viewModel::setUse24HourClock
                        )
                    }
                }

                // 2. DYNAMIC ACCENT COLOR & MATERIAL YOU
                item {
                    SectionLabel("02 // ACCENT & MATERIAL YOU")
                    Spacer(modifier = Modifier.height(8.dp))
                    AccentColorPickerSection(
                        currentAccentArgb = settings.accentColorArgb,
                        useMaterialYouAccent = settings.useMaterialYouAccent,
                        onToggleMaterialYou = viewModel::setUseMaterialYouAccent,
                        onAccentSelected = viewModel::setAccentColor
                    )
                }

                // 3. ICON PACK MANAGER
                item {
                    SectionLabel("03 // ICON PACK MANAGER")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconModeOptionRow(
                            title = "Kanso Built-In (Monochrome + Accent)",
                            subtitle = "Default industrial glyphs tinted with your active accent",
                            selected = settings.iconPackMode == IconPackMode.KANSO_ACCENT,
                            onClick = { viewModel.setIconPack(IconPackMode.KANSO_ACCENT) }
                        )
                        IconModeOptionRow(
                            title = "Kanso Built-In (Pure Monochrome)",
                            subtitle = "Adaptive glyphs tinted to matte hardware ink",
                            selected = settings.iconPackMode == IconPackMode.KANSO_MONOCHROME,
                            onClick = { viewModel.setIconPack(IconPackMode.KANSO_MONOCHROME) }
                        )
                        IconModeOptionRow(
                            title = "System Default Icons",
                            subtitle = "Original full-color application icons",
                            selected = settings.iconPackMode == IconPackMode.SYSTEM_DEFAULT,
                            onClick = { viewModel.setIconPack(IconPackMode.SYSTEM_DEFAULT) }
                        )

                        if (installedPacks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "INSTALLED THIRD-PARTY ICON PACKS (${installedPacks.size})",
                                color = colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp
                            )
                            installedPacks.forEach { pack ->
                                ThirdPartyPackRow(
                                    pack = pack,
                                    selected = settings.iconPackMode == IconPackMode.THIRD_PARTY &&
                                        settings.selectedIconPackPackage == pack.packageName,
                                    viewModel = viewModel,
                                    onClick = {
                                        viewModel.setIconPack(IconPackMode.THIRD_PARTY, pack.packageName)
                                    }
                                )
                            }
                        } else {
                            Text(
                                text = "No third-party ADW/Nova/Apex icon packs installed yet.",
                                color = colors.inkSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ActionPillButton(
                                icon = Icons.Rounded.Download,
                                label = "Install Icon Packs",
                                highlight = true,
                                modifier = Modifier
                                    .testTag("install_icon_packs_button")
                                    .weight(1.4f),
                                onClick = {
                                    val playIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        "market://search?q=icon+pack&c=apps".toUri()
                                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    runCatching { context.startActivity(playIntent) }.onFailure {
                                        val webIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            "https://play.google.com/store/search?q=icon%20pack&c=apps".toUri()
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                        runCatching { context.startActivity(webIntent) }
                                    }
                                }
                            )
                            ActionPillButton(
                                icon = Icons.Rounded.Refresh,
                                label = "Rescan",
                                modifier = Modifier
                                    .testTag("rescan_icon_packs_button")
                                    .weight(1f),
                                onClick = { viewModel.refreshIconPacks() }
                            )
                        }
                    }
                }

                // 4. HOME, DOCK, WEATHER & LOCKSCREEN
                item {
                    SectionLabel("04 // HOME, DOCK & LOCKSCREEN")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SettingsToggleRow(
                            title = "Home Screen Search Bar",
                            subtitle = "Compact bar above the Calendar ribbon on Page 1",
                            checked = settings.showHomeSearchBar,
                            testTag = "toggle_home_search_bar",
                            onCheckedChange = viewModel::setShowHomeSearchBar
                        )

                        // Workspace Screens (1..6)
                        CountSelectorRow(
                            title = "Workspace Pages",
                            subtitle = "Extra modular grid screens (or tap + Page on Workspace)",
                            options = listOf(1, 2, 3, 4),
                            selectedValue = settings.workspacePageCount.coerceAtMost(4),
                            testTagPrefix = "workspace_pages",
                            onSelect = viewModel::setWorkspacePageCount
                        )

                        // Home & Drawer Grid Columns (4 or 5)
                        CountSelectorRow(
                            title = "Workspace & Drawer Columns",
                            subtitle = "4 or 5 columns with minimal icon padding",
                            options = listOf(4, 5),
                            selectedValue = settings.homeGridColumns,
                            testTagPrefix = "grid_cols",
                            onSelect = viewModel::setHomeGridColumns
                        )

                        // Dock Slot Count (4 or 5)
                        CountSelectorRow(
                            title = "Floating Dock Slots",
                            subtitle = "Long-press any dock slot on Home to swap apps",
                            options = listOf(4, 5),
                            selectedValue = settings.dockSlotCount,
                            testTagPrefix = "dock_count",
                            onSelect = viewModel::setDockSlotCount
                        )

                        // Weather Auto-Location + Manual Open-Meteo Picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Weather: ${settings.weather.cityName} (${settings.weather.temperatureC}°)",
                                    color = colors.inkPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.MyLocation,
                                        contentDescription = "Use current location",
                                        tint = if (settings.weather.isUsingAutoLocation) colors.accent else colors.inkSecondary,
                                        modifier = Modifier
                                            .testTag("weather_auto_location_btn")
                                            .size(20.dp)
                                            .kansoTactileClickable {
                                                if (!health.coarseLocationGranted) {
                                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                                } else {
                                                    viewModel.enableAutoLocationWeather()
                                                }
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh weather",
                                        tint = colors.accent,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .kansoTactileClickable { viewModel.refreshWeather(force = true) }
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cityQuery,
                                    onValueChange = { cityQuery = it },
                                    singleLine = true,
                                    label = { Text("Override city (Open-Meteo)") },
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .testTag("search_weather_city_button")
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                        .kansoTactileClickable { viewModel.searchWeatherCities(cityQuery) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search city",
                                        tint = colors.onAccent
                                    )
                                }
                            }
                            cityResults.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .kansoNeumorphic(raised = false, cornerRadius = 12.dp)
                                        .kansoTactileClickable {
                                            viewModel.selectWeatherCity(option)
                                            cityQuery = ""
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${option.name}, ${option.country}",
                                        color = colors.inkPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Select",
                                        color = colors.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Force Kanso Lockscreen over stock lockscreen via Accessibility (All Devices)
                        SettingsToggleRow(
                            title = "Force Kanso Lockscreen (Accessibility)",
                            subtitle = "Overrides stock lockscreen on all devices when Accessibility is enabled",
                            checked = settings.lockscreenCompanionEnabled,
                            testTag = "toggle_lockscreen_companion",
                            onCheckedChange = { enabled ->
                                viewModel.setLockscreenCompanionEnabled(context, enabled)
                            }
                        )

                        // Force Kanso Multitasking / Recents over stock via Accessibility (All Devices)
                        SettingsToggleRow(
                            title = "Force Kanso Multitasking (Accessibility)",
                            subtitle = "Intercepts stock Recents with Kanso window previews on all devices",
                            checked = settings.forceRecentsOverrideEnabled,
                            testTag = "toggle_force_recents_override",
                            onCheckedChange = viewModel::setForceRecentsOverrideEnabled
                        )

                        if ((settings.lockscreenCompanionEnabled || settings.forceRecentsOverrideEnabled) &&
                            !health.accessibilityGranted
                        ) {
                            ActionPillButton(
                                icon = Icons.Rounded.VerifiedUser,
                                label = "Enable Accessibility Service",
                                highlight = true,
                                modifier = Modifier
                                    .testTag("enable_accessibility_override_btn")
                                    .fillMaxWidth(),
                                onClick = { RomDetector.openAccessibilitySettings(context) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionPillButton(
                                icon = Icons.Rounded.Edit,
                                label = "Edit Lock",
                                highlight = settings.lockscreenCompanionEnabled,
                                modifier = Modifier
                                    .testTag("edit_lockscreen_button")
                                    .weight(1f),
                                onClick = {
                                    val intent = Intent(context, LockscreenActivity::class.java).apply {
                                        putExtra(LockscreenActivity.EXTRA_START_EDIT_MODE, true)
                                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                            ActionPillButton(
                                icon = Icons.Rounded.Lock,
                                label = "Preview Lock",
                                modifier = Modifier
                                    .testTag("preview_lockscreen_button")
                                    .weight(1f),
                                onClick = {
                                    context.startActivity(
                                        Intent(context, LockscreenActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                        }
                                    )
                                }
                            )
                            ActionPillButton(
                                icon = Icons.Rounded.NightsStay,
                                label = "Recents",
                                modifier = Modifier
                                    .testTag("preview_recents_button")
                                    .weight(0.95f),
                                onClick = {
                                    context.startActivity(
                                        Intent(context, RecentsActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                        }
                                    )
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionPillButton(
                                icon = Icons.Rounded.Home,
                                label = if (health.isDefaultHomeLauncher) "Default Home Active" else "Set Default Home",
                                highlight = !health.isDefaultHomeLauncher,
                                modifier = Modifier
                                    .testTag("set_default_home_button")
                                    .weight(1.3f),
                                onClick = {
                                    if (health.isChineseRom) {
                                        RomDetector.openAccessibilitySettings(context)
                                    } else {
                                        val intent = RomDetector.buildDefaultLauncherIntent(context)
                                        runCatching { defaultHomeRoleLauncher.launch(intent) }.onFailure {
                                            RomDetector.requestDefaultLauncher(context)
                                        }
                                    }
                                }
                            )
                            ActionPillButton(
                                icon = Icons.Rounded.RestartAlt,
                                label = "Reset Home",
                                modifier = Modifier
                                    .testTag("reset_home_layout_button")
                                    .weight(1f),
                                onClick = { viewModel.resetPage1Layout() }
                            )
                        }
                    }
                }

                // 4B. 60FPS CPU FRAME-BUDGET GOVERNOR
                item {
                    SectionLabel("04B // 60FPS PERFORMANCE GOVERNOR")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .testTag("settings_performance_governor_card")
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsToggleRow(
                            title = "60FPS Animation CPU Governor",
                            subtitle = "Limits background telemetry & FFT CPU usage during animations while preserving mecha-industrial fidelity",
                            checked = settings.performanceGovernorEnabled,
                            testTag = "toggle_performance_governor",
                            onCheckedChange = { enabled ->
                                viewModel.setPerformanceGovernorEnabled(context, enabled)
                            }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "${governor.fpsEstimate} FPS",
                                    color = colors.inkPrimary,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = governor.tier.badge,
                                color = colors.accent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "CPU ${governor.cpuBudgetPercent}%",
                                color = colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                // 5. HIDDEN APPS MANAGER
                if (hiddenPackages.isNotEmpty()) {
                    item {
                        SectionLabel("05 // HIDDEN APPS (${hiddenPackages.size})")
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val hiddenApps = allApps.filter { it.packageName in hiddenPackages }
                            hiddenApps.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = app.label,
                                        color = colors.inkPrimary,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.accent)
                                            .kansoTactileClickable { viewModel.unhideApp(app.packageName) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Visibility,
                                            contentDescription = "Unhide ${app.label}",
                                            tint = colors.onAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Unhide", color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. DUAL-ROM SYSTEM & PERMISSIONS HEALTH CENTER
                item {
                    SectionLabel("06 // PERMISSIONS & DUAL-ROM SETUP")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Detected ROM Strategy Banner + One-Tap Runtime Permission Request
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = health.romRegionLabel,
                                    color = colors.accent,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = if (health.isChineseRom) {
                                        "CN OEM strategy: Accessibility Home/Recents + Overlay"
                                    } else {
                                        "Global ROM strategy: Android RoleManager Default Home"
                                    },
                                    color = colors.inkSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        ActionPillButton(
                            icon = Icons.Rounded.VerifiedUser,
                            label = "Request All Runtime Permissions",
                            highlight = true,
                            modifier = Modifier
                                .testTag("request_all_runtime_perms_btn")
                                .fillMaxWidth(),
                            onClick = {
                                val perms = buildList {
                                    add(Manifest.permission.RECORD_AUDIO)
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        add(Manifest.permission.BLUETOOTH_CONNECT)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                                allRuntimePermissionsLauncher.launch(perms)
                            }
                        )

                        PermissionHealthRow(
                            title = "Default Home App (Launcher)",
                            subtitle = "Categorized as Home App via RoleManager & Android Home settings",
                            granted = health.isDefaultHomeLauncher,
                            testTag = "perm_default_home",
                            onGrantClick = {
                                val intent = RomDetector.buildDefaultLauncherIntent(context)
                                runCatching { defaultHomeRoleLauncher.launch(intent) }.onFailure {
                                    RomDetector.requestDefaultLauncher(context)
                                }
                            }
                        )
                        PermissionHealthRow(
                            title = "Do Not Disturb Policy Access",
                            subtitle = "Required for 3-state hardware Silent / Vibrate / Ring sound switch",
                            granted = health.dndPolicyGranted,
                            testTag = "perm_dnd_policy",
                            onGrantClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            }
                        )
                        PermissionHealthRow(
                            title = "Notification & Media Access",
                            subtitle = "Powers Media HUD (Spotify/YT/Apple), Notification Panel & badges",
                            granted = health.notificationAccessGranted,
                            testTag = "perm_notification_access",
                            onGrantClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }
                            }
                        )
                        PermissionHealthRow(
                            title = "Modify System Settings",
                            subtitle = "Allows Control Panel Brightness slider & Auto-Rotate toggle",
                            granted = health.writeSettingsGranted,
                            testTag = "perm_write_settings",
                            onGrantClick = {
                                viewModel.telemetryRepository.openWriteSettingsPermission()
                            }
                        )
                        PermissionHealthRow(
                            title = "Accessibility Lock & Multitasking Service",
                            subtitle = "Forces Kanso Lockscreen & Multitasking previews over stock UI on all devices",
                            granted = health.accessibilityGranted,
                            testTag = "perm_accessibility_service",
                            onGrantClick = { RomDetector.openAccessibilitySettings(context) }
                        )
                        PermissionHealthRow(
                            title = "Usage Access (Recents Multitasking)",
                            subtitle = "Populates recent apps for Kanso Recents switcher on OEM ROMs",
                            granted = health.usageStatsGranted,
                            testTag = "perm_usage_stats",
                            onGrantClick = { RomDetector.openUsageAccessSettings(context) }
                        )
                        PermissionHealthRow(
                            title = "Display Over Other Apps",
                            subtitle = "Enables overlay hooks & optional lockscreen companion on OEM ROMs",
                            granted = health.overlayGranted,
                            testTag = "perm_system_overlay",
                            onGrantClick = { RomDetector.openOverlaySettings(context) }
                        )
                        PermissionHealthRow(
                            title = "Live Audio FFT Visualizer",
                            subtitle = "Real-time audio spectrum for the dotted line",
                            granted = health.audioVisualizerGranted,
                            testTag = "perm_audio_visualizer",
                            onGrantClick = {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                        PermissionHealthRow(
                            title = "Bluetooth Connect",
                            subtitle = "Reads live Bluetooth state on Android 12+",
                            granted = health.bluetoothConnectGranted,
                            testTag = "perm_bluetooth_connect",
                            onGrantClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                }
                            }
                        )
                        PermissionHealthRow(
                            title = "Coarse Location",
                            subtitle = "Auto-updates local Open-Meteo weather & city name",
                            granted = health.coarseLocationGranted,
                            testTag = "perm_coarse_location",
                            onGrantClick = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        )
                        PermissionHealthRow(
                            title = "Write Secure Settings (ADB / OEM)",
                            subtitle = if (health.writeSecureSettingsGranted) {
                                "Granted — advanced system toggles unlocked"
                            } else {
                                "Optional ADB grant: pm grant com.kanso.launcher android.permission.WRITE_SECURE_SETTINGS"
                            },
                            granted = health.writeSecureSettingsGranted,
                            testTag = "perm_write_secure_settings",
                            onGrantClick = { viewModel.telemetryRepository.openSystemSettings() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountSelectorRow(
    title: String,
    subtitle: String,
    options: List<Int>,
    selectedValue: Int,
    testTagPrefix: String,
    onSelect: (Int) -> Unit
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = colors.inkPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = colors.inkSecondary,
                fontSize = 11.5.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { count ->
                val selected = selectedValue == count
                Box(
                    modifier = Modifier
                        .testTag("${testTagPrefix}_$count")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (selected) colors.accent else colors.surfaceInset)
                        .kansoTactileClickable { onSelect(count) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        color = if (selected) colors.onAccent else colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalKansoColors.current
    Text(
        text = text,
        color = colors.inkSecondary,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = colors.inkPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = colors.inkSecondary,
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun IconModeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kansoNeumorphic(raised = !selected, cornerRadius = 16.dp)
            .kansoTactileClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.inkPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
            Text(
                text = subtitle,
                color = colors.inkSecondary,
                fontSize = 11.5.sp
            )
        }
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else colors.trackMuted)
        )
    }
}

@Composable
private fun ThirdPartyPackRow(
    pack: IconPackInfo,
    selected: Boolean,
    viewModel: LauncherViewModel,
    onClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    var previews by remember(pack.packageName) { mutableStateOf<List<ImageBitmap>>(emptyList()) }

    LaunchedEffect(pack.packageName) {
        previews = viewModel.iconPackManager.loadPackPreviewBitmaps(pack.packageName, 4)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .kansoNeumorphic(raised = !selected, cornerRadius = 16.dp)
            .kansoTactileClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pack.label,
                color = colors.inkPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.accent else colors.trackMuted)
            )
        }
        if (previews.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                previews.forEach { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlight) colors.accent else colors.darkModuleBg)
            .kansoTactileClickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlight) colors.onAccent else colors.accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = if (highlight) colors.onAccent else Color(0xFFF2F3F5),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PermissionHealthRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    testTag: String,
    onGrantClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    val badgeColor = if (granted) colors.statusHealthy else colors.statusWarning

    Row(
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.inkPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            )
            Text(
                text = subtitle,
                color = colors.inkSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (granted) badgeColor.copy(alpha = 0.16f) else colors.accent)
                .kansoTactileClickable(enabled = !granted, onClick = onGrantClick)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = if (granted) "ACTIVE" else "GRANT",
                color = if (granted) badgeColor else colors.onAccent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}
