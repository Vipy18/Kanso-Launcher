package com.kanso.launcher.ui.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.R
import com.kanso.launcher.data.db.HomeItemEntity
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.service.KansoNotificationItem
import com.kanso.launcher.service.KansoNotificationListener
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.home.ModularGridCanvas
import com.kanso.launcher.ui.home.ModularWidgetConfigDialog
import com.kanso.launcher.ui.panels.KansoShadeOverlay
import com.kanso.launcher.ui.panels.ShadeStage
import com.kanso.launcher.ui.theme.DotMatrixFont
import com.kanso.launcher.ui.theme.GeistFontFamily
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.KansoTheme
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceMonoFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.ModularWidgetAddDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LockscreenActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_EDIT_MODE = "extra_start_edit_mode"
    }

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application as KansoApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        disableOsTransitions(opening = true)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val startInEditMode = intent?.getBooleanExtra(EXTRA_START_EDIT_MODE, false) ?: false

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            KansoTheme(
                themeMode = ThemeMode.DARK,
                accentColorArgb = settings.accentColorArgb,
                useMaterialYouAccent = settings.useMaterialYouAccent
            ) {
                LockscreenScreen(
                    viewModel = viewModel,
                    initialEditMode = startInEditMode,
                    onUnlock = { requestUnlock() }
                )
            }
        }
    }

    override fun finish() {
        super.finish()
        disableOsTransitions(opening = false)
    }

    private fun disableOsTransitions(opening: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                if (opening) OVERRIDE_TRANSITION_OPEN else OVERRIDE_TRANSITION_CLOSE,
                0,
                0
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun requestUnlock() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km != null && km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    finish()
                }
            })
        } else {
            finish()
        }
    }
}

@Composable
private fun LockscreenScreen(
    viewModel: LauncherViewModel,
    initialEditMode: Boolean,
    onUnlock: () -> Unit
) {
    val colors = LocalKansoColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val health by viewModel.permissionsHealth.collectAsStateWithLifecycle()
    val media by viewModel.mediaState.collectAsStateWithLifecycle()
    val badges by viewModel.notificationBadges.collectAsStateWithLifecycle()
    val notifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val audioBands by viewModel.audioBands.collectAsStateWithLifecycle()
    val allHomeItems by viewModel.homeItems.collectAsStateWithLifecycle()
    val folders by viewModel.foldersWithApps.collectAsStateWithLifecycle()
    val overrides by viewModel.iconOverrides.collectAsStateWithLifecycle()
    val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()

    val lockscreenItems = remember(allHomeItems) {
        allHomeItems.filter { it.pageIndex == SurfacePage.LOCKSCREEN }
    }

    var isEditMode by remember { mutableStateOf(initialEditMode) }
    var shadeStage by remember { mutableStateOf(ShadeStage.CLOSED) }
    var showWidgetCatalog by remember { mutableStateOf(false) }
    var editingWidget by remember { mutableStateOf<HomeItemEntity?>(null) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var lastTouchFractionX by remember { mutableFloatStateOf(0.25f) }

    BackHandler(enabled = isEditMode || shadeStage != ShadeStage.CLOSED) {
        when {
            shadeStage != ShadeStage.CLOSED -> shadeStage = ShadeStage.CLOSED
            isEditMode -> isEditMode = false
        }
    }

    val scope = rememberCoroutineScope()
    val revealProgress = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium)
        )
    }

    val smoothUnlock: () -> Unit = {
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                revealProgress.animateTo(0f, animationSpec = tween(durationMillis = 120))
                onUnlock()
            }
        }
    }

    val openNotificationAccess = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        Unit
    }

    val pulseTransition = rememberInfiniteTransition(label = "lockscreen_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_pulse"
    )
    val chevronOffsetY by pulseTransition.animateFloat(
        initialValue = 2f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "unlock_chevron"
    )

    val hours = if (settings.use24HourClock) telemetry.clock.hours24 else telemetry.clock.hours12
    val weatherCondition = remember(settings.weather.weatherCode) {
        weatherCodeLabel(settings.weather.weatherCode)
    }
    val ramPct = (telemetry.ramUsageFraction * 100f).roundToInt().coerceIn(1, 99)

    Box(
        modifier = Modifier
            .testTag("lockscreen_root")
            .fillMaxSize()
            .graphicsLayer {
                val p = revealProgress.value.coerceIn(0f, 1f)
                alpha = p
                val s = 1.035f - 0.035f * p
                scaleX = s
                scaleY = s
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0B0D), Color(0xFF0D0E10), Color(0xFF090A0C))
                )
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.firstOrNull { it.pressed }
                        if (down != null && size.width > 0) {
                            lastTouchFractionX = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isEditMode = !isEditMode
                    }
                )
            }
            .pointerInput(shadeStage, isEditMode) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onVerticalDrag = { _, dragAmount -> dragAccumulator += dragAmount },
                    onDragEnd = {
                        if (!isEditMode) {
                            when {
                                dragAccumulator < -115f && shadeStage == ShadeStage.CLOSED -> smoothUnlock()
                                dragAccumulator > 105f && shadeStage == ShadeStage.CLOSED -> {
                                    shadeStage = if (lastTouchFractionX >= 0.5f) {
                                        ShadeStage.CONTROLS
                                    } else {
                                        ShadeStage.NOTIFICATIONS
                                    }
                                }
                            }
                        }
                        dragAccumulator = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HUD Sub-Header: SECURE_BOOT // ARMED + LOCKED + Edit Mode Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1A1B1E))
                        .border(0.75.dp, colors.accent.copy(alpha = 0.25f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = if (isEditMode) "LOCKSCREEN // EDITOR" else "SECURE_BOOT // ARMED",
                        color = colors.accent,
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        letterSpacing = 1.1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isEditMode) {
                        Row(
                            modifier = Modifier
                                .testTag("lockscreen_add_widget_btn")
                                .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.5f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF1F2128))
                                .kansoTactileClickable { showWidgetCatalog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add lockscreen widget",
                                tint = colors.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "WIDGET",
                                color = Color(0xFFF2F3F5),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .testTag("lockscreen_edit_done_btn")
                                .size(34.dp)
                                .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.5f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors.accent)
                                .kansoTactileClickable { isEditMode = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Finish lockscreen editing",
                                tint = colors.onAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1A1B1E))
                                .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "LOCKED",
                                color = Color(0xFFE3E2E4),
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .testTag("lockscreen_edit_toggle_btn")
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1A1B1E))
                                .border(0.75.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                .kansoTactileClickable { isEditMode = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Customize lockscreen widgets",
                                tint = colors.accent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF504533).copy(alpha = 0.28f))
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Stitch Horizontal Dot-Matrix Clock + Recessed Date & Weather Horizon Pill
            StitchHorizontalDotMatrixClock(
                hours = hours,
                minutes = telemetry.clock.minutes,
                pixelSize = 6.2.dp,
                pixelGap = 2.0.dp,
                digitColor = Color(0xFFEAECEF),
                colonColor = colors.accent
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .testTag("lockscreen_horizon_pill")
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1A1B1E))
                    .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.35f), RoundedCornerShape(50))
                    .kansoTactileClickable { viewModel.refreshWeather(force = true) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Text(
                    text = telemetry.clock.formattedDate.uppercase(),
                    color = Color(0xFFE3E2E4),
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "•",
                    color = Color(0xFF8C8477),
                    fontSize = 10.sp
                )
                Icon(
                    imageVector = if (settings.weather.weatherCode <= 2) Icons.Rounded.WbSunny else Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${settings.weather.temperatureC}°C $weatherCondition",
                    color = Color(0xFFD4C4AC),
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Middle Telemetry & Widget Zone (Full Stitch AOD stack OR Custom Modular Grid)
            if (isEditMode || lockscreenItems.isNotEmpty()) {
                if (!isEditMode && notifications.isNotEmpty()) {
                    GlanceableGlyphRow(
                        item = notifications.first(),
                        accentColor = colors.accent,
                        onClick = { shadeStage = ShadeStage.NOTIFICATIONS }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                ModularGridCanvas(
                    items = lockscreenItems,
                    columns = 4,
                    cellHeight = 76.dp,
                    minRows = 3,
                    folders = folders,
                    overrides = overrides,
                    badges = badges,
                    iconPackManager = viewModel.iconPackManager,
                    iconPackMode = settings.iconPackMode,
                    selectedIconPackPackage = settings.selectedIconPackPackage,
                    telemetry = telemetry,
                    media = media,
                    weather = settings.weather,
                    audioBands = audioBands,
                    notificationAccessGranted = health.notificationAccessGranted,
                    appWidgetHost = null,
                    isEditMode = isEditMode,
                    onEnterEditMode = { isEditMode = true },
                    onLaunchApp = { pkg, act ->
                        viewModel.appRepository.launchApp(pkg, act)
                        onUnlock()
                    },
                    onOpenFolder = {},
                    onExecuteAction = { action, pkg, act ->
                        viewModel.executeAction(context, action, pkg, act)
                    },
                    onMediaPlayPause = { KansoNotificationListener.togglePlayPause(context) },
                    onMediaPrev = { KansoNotificationListener.skipPrevious(context) },
                    onMediaNext = { KansoNotificationListener.skipNext(context) },
                    onOpenPlayerApp = { pkg ->
                        viewModel.appRepository.launchApp(pkg, "")
                        onUnlock()
                    },
                    onRequestMediaPermission = openNotificationAccess,
                    onWeatherClick = { viewModel.refreshWeather(force = true) },
                    onCalendarClick = {
                        viewModel.appRepository.launchRoleFallback("CALENDAR")
                        onUnlock()
                    },
                    onSelectRingerMode = { mode -> viewModel.setRingerMode(mode) },
                    onSetBrightness = { frac -> viewModel.telemetryRepository.setBrightnessFraction(frac) },
                    onSetVolume = { frac -> viewModel.telemetryRepository.setMediaVolumeFraction(frac) },
                    use24HourClock = settings.use24HourClock,
                    onMoveItemToCell = { item, col, row ->
                        viewModel.moveHomeItemToGridCell(item, col, row, 4)
                    },
                    onConfigureItem = { editingWidget = it },
                    onRemoveItem = { viewModel.removeHomeItem(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // GLANCEABLE GLYPHS Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                            Text(
                                text = "GLANCEABLE GLYPHS",
                                color = Color(0xFFD4C4AC),
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.5.sp,
                                letterSpacing = 1.4.sp
                            )
                        }
                        Text(
                            text = if (notifications.isEmpty()) {
                                "0 ALERTS QUEUED"
                            } else {
                                "${notifications.size} ALERTS QUEUED"
                            },
                            color = Color(0xFF9D8F78),
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.9.sp
                        )
                    }

                    // Live Glanceable Notifications (up to 2 compact cards)
                    if (!health.notificationAccessGranted) {
                        Row(
                            modifier = Modifier
                                .testTag("lockscreen_notif_access_btn")
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1A1B1E))
                                .border(0.75.dp, colors.accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                                .kansoTactileClickable(onClick = openNotificationAccess)
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0xFF0D0E10)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ENABLE NOTIFICATION ACCESS",
                                    color = colors.accent,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "Tap to display live alerts & media deck on lockscreen",
                                    color = Color(0xFFB5BAC4),
                                    fontFamily = GeistFontFamily,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF9D8F78),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else if (notifications.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF18191C))
                                .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .kansoTactileClickable { shadeStage = ShadeStage.NOTIFICATIONS }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0D0E10)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF9D8F78),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "ALL SYSTEMS QUIET // PULL DOWN FOR SHADE",
                                color = Color(0xFF9D8F78),
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.9.sp
                            )
                        }
                    } else {
                        notifications.take(2).forEachIndexed { idx, notif ->
                            GlanceableGlyphRow(
                                item = notif,
                                accentColor = if (idx == 0) colors.accent else colors.secondaryTelemetry,
                                onClick = { shadeStage = ShadeStage.NOTIFICATIONS }
                            )
                        }
                    }

                    // Mini Media Player Deck
                    Row(
                        modifier = Modifier
                            .testTag("lockscreen_media_deck")
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1A1B1E))
                            .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                            .kansoMechaAccents(accentActive = media.isPlaying, showCornerBrackets = true, showLedPip = false)
                            .kansoTactileClickable {
                                if (media.packageName.isNotBlank()) {
                                    viewModel.appRepository.launchApp(media.packageName, "")
                                    onUnlock()
                                } else {
                                    KansoNotificationListener.togglePlayPause(context)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Recessed 3-bar EQ pod
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFF0D0E10)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                modifier = Modifier.height(15.dp)
                            ) {
                                val b0 = if (media.isPlaying) (audioBands.getOrNull(1) ?: 0.55f).coerceIn(0.25f, 1f) else 0.32f
                                val b1 = if (media.isPlaying) (audioBands.getOrNull(3) ?: 0.85f).coerceIn(0.35f, 1f) else 0.65f
                                val b2 = if (media.isPlaying) (audioBands.getOrNull(5) ?: 0.45f).coerceIn(0.20f, 1f) else 0.25f
                                listOf(b0, b1, b2).forEach { level ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height((14f * level).dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(colors.accent)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = media.title.ifBlank { "MEDIA BUS // STANDBY" },
                                color = colors.accent,
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = media.artist.ifBlank { telemetry.audioOutputLabel },
                                color = Color(0xFFB5BAC4),
                                fontFamily = GeistFontFamily,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .testTag("lockscreen_media_play_btn")
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B2D31))
                                    .kansoTactileClickable {
                                        KansoNotificationListener.togglePlayPause(context)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (media.isPlaying) "Pause media" else "Play media",
                                    tint = Color(0xFFF2F3F5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .testTag("lockscreen_media_next_btn")
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B2D31))
                                    .kansoTactileClickable {
                                        KansoNotificationListener.skipNext(context)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next track",
                                    tint = Color(0xFFF2F3F5),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Dual Telemetry Pods (2-column grid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left Pod: Weather & Sector
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1A1B1E))
                                .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                                .kansoTactileClickable { viewModel.refreshWeather(force = true) }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${settings.weather.temperatureC}°C // ${settings.weather.humidityPercent}% RH",
                                    color = Color(0xFFE3E2E4),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                                Icon(
                                    imageVector = if (settings.weather.weatherCode <= 2) Icons.Rounded.WbSunny else Icons.Rounded.Cloud,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "${settings.weather.cityName.uppercase()} // $weatherCondition",
                                color = Color(0xFF9D8F78),
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 8.sp,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right Pod: System Vitals
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1A1B1E))
                                .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                                .kansoTactileClickable { shadeStage = ShadeStage.CONTROLS }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Memory,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "KANSO OS v4.2",
                                        color = colors.accent,
                                        fontFamily = SpaceMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.secondaryTelemetry.copy(alpha = pulseAlpha))
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$ramPct% RAM",
                                    color = colors.secondaryTelemetry,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp
                                )
                                Text(
                                    text = "${telemetry.batteryPercent}% PWR",
                                    color = Color(0xFF9D8F78),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Bottom Hardware Shortcut Row (Torch • Swipe Up Unlock • Camera)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Tactile Torch Button
                Box(
                    modifier = Modifier
                        .testTag("lockscreen_torch_btn")
                        .size(50.dp)
                        .kansoNeumorphic(
                            raised = true,
                            cornerRadius = 25.dp,
                            surfaceColor = if (telemetry.isTorchEnabled) colors.accent else Color(0xFF1E1F23)
                        )
                        .border(
                            width = 1.dp,
                            color = if (telemetry.isTorchEnabled) colors.accent else Color(0xFF504533).copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .kansoTactileClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.telemetryRepository.toggleTorch()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashlightOn,
                        contentDescription = "Toggle flashlight",
                        tint = if (telemetry.isTorchEnabled) colors.onAccent else Color(0xFFD4C4AC),
                        modifier = Modifier.size(21.dp)
                    )
                }

                // Center Unlock Telemetry Prompt
                Column(
                    modifier = Modifier
                        .testTag("lockscreen_unlock_pill")
                        .clip(RoundedCornerShape(16.dp))
                        .kansoTactileClickable(onClick = smoothUnlock)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { translationY = chevronOffsetY }
                    )
                    Text(
                        text = stringResource(R.string.swipe_up_to_unlock).uppercase(),
                        color = Color(0xFFD4C4AC),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.3.sp
                    )
                }

                // Circular Tactile Camera Button
                Box(
                    modifier = Modifier
                        .testTag("lockscreen_camera_btn")
                        .size(50.dp)
                        .kansoNeumorphic(
                            raised = true,
                            cornerRadius = 25.dp,
                            surfaceColor = Color(0xFF1E1F23)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFF504533).copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .kansoTactileClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.appRepository.launchRoleFallback("CAMERA")
                            onUnlock()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = "Open camera",
                        tint = Color(0xFFD4C4AC),
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            // Home Gesture Bar Indicator
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(width = 116.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.32f))
            )
        }

        // Split Pull-Down Notification & Control Panel Shade
        KansoShadeOverlay(
            stage = shadeStage,
            notifications = notifications,
            notificationAccessGranted = health.notificationAccessGranted,
            telemetry = telemetry,
            media = media,
            controlTiles = settings.controlPanelTiles,
            apps = visibleApps,
            iconPackManager = viewModel.iconPackManager,
            iconPackMode = settings.iconPackMode,
            selectedIconPackPackage = settings.selectedIconPackPackage,
            overrides = overrides,
            onStageChange = { shadeStage = it },
            onOpenNotificationAccessSettings = openNotificationAccess,
            onDismissNotification = { key -> KansoNotificationListener.cancelNotificationByKey(key) },
            onClearAllNotifications = { KansoNotificationListener.clearAllNotifications() },
            onExecuteTile = { tile ->
                viewModel.executeAction(
                    context = context,
                    action = tile.action,
                    targetPackage = tile.packageName,
                    targetActivity = tile.activityName
                )
            },
            onAddControlTile = { viewModel.addControlPanelTile(it) },
            onUpdateControlTile = { idx, tile -> viewModel.updateControlPanelTile(idx, tile) },
            onRemoveControlTile = { idx -> viewModel.removeControlPanelTile(idx) },
            onResetControlTiles = { viewModel.resetControlPanelTiles() },
            onBrightnessChange = { viewModel.telemetryRepository.setBrightnessFraction(it) },
            onVolumeChange = { viewModel.telemetryRepository.setMediaVolumeFraction(it) },
            onPlayPause = { KansoNotificationListener.togglePlayPause(context) },
            onPrevTrack = { KansoNotificationListener.skipPrevious(context) },
            onNextTrack = { KansoNotificationListener.skipNext(context) },
            onOpenPlayerApp = { pkg ->
                viewModel.appRepository.launchApp(pkg, "")
                onUnlock()
            },
            onOpenAudioOutputSettings = { viewModel.openAudioOutputSettings() }
        )
    }

    if (showWidgetCatalog) {
        ModularWidgetAddDialog(
            telemetry = telemetry,
            media = media,
            weather = settings.weather,
            audioBands = audioBands,
            providers = emptyList(),
            allowExternalWidgets = false,
            onAddKansoWidget = { tmpl ->
                viewModel.addModularWidget(
                    itemType = tmpl.itemType,
                    label = tmpl.title,
                    spanCols = tmpl.spanCols,
                    spanRows = tmpl.spanRows,
                    config = tmpl.config,
                    pageIndex = SurfacePage.LOCKSCREEN
                )
            },
            onSelectExternalProvider = {},
            onDismiss = { showWidgetCatalog = false }
        )
    }

    editingWidget?.let { item ->
        ModularWidgetConfigDialog(
            item = item,
            maxCols = 4,
            apps = visibleApps,
            iconPackManager = viewModel.iconPackManager,
            iconPackMode = settings.iconPackMode,
            selectedIconPackPackage = settings.selectedIconPackPackage,
            overrides = overrides,
            onSaveConfig = { cfg, spanCols, spanRows, label ->
                viewModel.updateHomeItemConfig(item, cfg, label)
                viewModel.resizeHomeItem(item, spanCols, spanRows, 4)
            },
            onRemoveWidget = { viewModel.removeHomeItem(item) },
            onDismiss = { editingWidget = null }
        )
    }
}

@Composable
private fun GlanceableGlyphRow(
    item: KansoNotificationItem,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B1C1F))
            .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .kansoTactileClickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF0D0E10)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.appName.uppercase(),
                    color = accentColor,
                    fontFamily = SpaceMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "• ${formatShortRelativeTime(item.postTimeMs)}",
                    color = Color(0xFF9D8F78),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 8.sp
                )
            }
            Text(
                text = listOf(item.title, item.text).filter { it.isNotBlank() }.joinToString(" — "),
                color = Color(0xFFD6D8DE),
                fontFamily = GeistFontFamily,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9D8F78),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun StitchHorizontalDotMatrixClock(
    hours: String,
    minutes: String,
    pixelSize: Dp,
    pixelGap: Dp,
    digitColor: Color,
    colonColor: Color
) {
    val hSafe = hours.padStart(2, '0').takeLast(2)
    val mSafe = minutes.padStart(2, '0').takeLast(2)
    val pitch = pixelSize + pixelGap

    // 4 digits (5 cols each) + 2 inner gaps (1.5 cols) + colon center block (4.2 cols)
    val totalCols = 5f * 4f + 1.5f * 2f + 4.4f
    val totalWidth = pitch * totalCols
    val totalHeight = pitch * 7f - pixelGap

    Canvas(modifier = Modifier.size(width = totalWidth, height = totalHeight)) {
        val px = pixelSize.toPx()
        val p = pitch.toPx()
        val cr = CornerRadius(px * 0.22f, px * 0.22f)

        fun drawDigit(ch: Char, offsetX: Float) {
            val grid = DotMatrixFont.bitmapFor(ch)
            for (r in grid.indices) {
                val row = grid[r]
                for (c in row.indices) {
                    if (row[c] == '#') {
                        drawRoundRect(
                            color = digitColor,
                            topLeft = Offset(offsetX + c * p, r * p),
                            size = Size(px, px),
                            cornerRadius = cr
                        )
                    }
                }
            }
        }

        val x0 = 0f
        val x1 = 6.5f * p
        val colonX = 12.6f * p
        val x2 = 16.0f * p
        val x3 = 22.5f * p

        drawDigit(hSafe[0], x0)
        drawDigit(hSafe[1], x1)

        // Two glowing vertical diamond '+' dots in Signal Amber
        val cross = DotMatrixFont.crossBitmap()
        val cPx = px * 0.72f
        val cPitch = p * 0.72f
        val cCr = CornerRadius(cPx * 0.25f, cPx * 0.25f)
        for (colonY in listOf(1.2f * p, 4.0f * p)) {
            for (r in cross.indices) {
                for (c in cross[r].indices) {
                    if (cross[r][c] == '#') {
                        drawRoundRect(
                            color = colonColor,
                            topLeft = Offset(colonX + c * cPitch, colonY + r * cPitch),
                            size = Size(cPx, cPx),
                            cornerRadius = cCr
                        )
                    }
                }
            }
        }

        drawDigit(mSafe[0], x2)
        drawDigit(mSafe[1], x3)
    }
}

private fun weatherCodeLabel(code: Int): String = when (code) {
    0 -> "CLEAR"
    1, 2 -> "PARTLY CLOUDY"
    3 -> "OVERCAST"
    45, 48 -> "FOG"
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> "RAIN"
    71, 73, 75, 85, 86 -> "SNOW"
    95, 96, 99 -> "STORM"
    else -> "AMBIENT"
}

private fun formatShortRelativeTime(postTimeMs: Long): String {
    val diffSec = ((System.currentTimeMillis() - postTimeMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "NOW"
        diffSec < 3600L -> "${diffSec / 60L}M AGO"
        diffSec < 86400L -> "${diffSec / 3600L}H AGO"
        else -> "${diffSec / 86400L}D AGO"
    }
}
