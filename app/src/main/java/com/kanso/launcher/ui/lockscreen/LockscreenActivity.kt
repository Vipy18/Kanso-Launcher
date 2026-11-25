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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.R
import com.kanso.launcher.data.db.HomeItemEntity
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.service.KansoNotificationListener
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.home.ModularGridCanvas
import com.kanso.launcher.ui.home.ModularWidgetConfigDialog
import com.kanso.launcher.ui.panels.KansoShadeOverlay
import com.kanso.launcher.ui.panels.ShadeStage
import com.kanso.launcher.ui.theme.DotMatrixClock
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.KansoTheme
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.ModularWidgetAddDialog
import kotlinx.coroutines.launch

/**
 * Clean Modular Mecha Lockscreen Activity (zero biometric HUD animations):
 * - Dot-matrix hero clock, notification pill, and customizable modular widget canvas
 * - Two-stage Notification & Control Panel pull-down shade
 * - Secure swipe-up unlock via KeyguardManager.requestDismissKeyguard()
 */
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

    BackHandler(enabled = isEditMode) {
        isEditMode = false
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
                revealProgress.animateTo(0f, animationSpec = tween(durationMillis = 125))
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

    val hours = if (settings.use24HourClock) telemetry.clock.hours24 else telemetry.clock.hours12
    val notificationCount = badges.values.sum()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = revealProgress.value.coerceIn(0f, 1f)
                alpha = p
                val s = 1.04f - 0.04f * p
                scaleX = s
                scaleY = s
            }
            .background(Color.Black)
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
                                dragAccumulator < -120f && shadeStage == ShadeStage.CLOSED -> smoothUnlock()
                                dragAccumulator > 110f -> {
                                    shadeStage = when (shadeStage) {
                                        ShadeStage.CLOSED -> ShadeStage.NOTIFICATIONS
                                        ShadeStage.NOTIFICATIONS -> ShadeStage.CONTROLS
                                        ShadeStage.CONTROLS -> ShadeStage.CONTROLS
                                    }
                                }
                            }
                        }
                        dragAccumulator = 0f
                    }
                )
            }
    ) {
        // Minimal curved accent line across top
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .align(Alignment.TopCenter)
        ) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.78f)
                cubicTo(
                    size.width * 0.42f, size.height * 0.78f,
                    size.width * 0.56f, size.height * 0.30f,
                    size.width, size.height * 0.30f
                )
            }
            drawPath(
                path = path,
                color = colors.accent,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top telemetry strip + Lockscreen Edit Mode pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "LOCKSCREEN EDITOR" else "KANSO LOCK",
                    color = colors.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${telemetry.batteryPercent}% BAT",
                        color = Color(0xFFB5BAC4),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    )
                    if (isEditMode) {
                        Box(
                            modifier = Modifier
                                .testTag("lockscreen_add_widget_btn")
                                .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.55f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF1F2128))
                                .kansoTactileClickable { showWidgetCatalog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Widget",
                                    color = Color(0xFFF2F3F5),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .testTag("lockscreen_edit_done_btn")
                                .size(34.dp)
                                .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.55f)
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
                        Box(
                            modifier = Modifier
                                .testTag("lockscreen_edit_toggle_btn")
                                .size(34.dp)
                                .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.55f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF18191E))
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

            Spacer(modifier = Modifier.height(26.dp))

            // Dot-Matrix Hero Clock + Date + Notification badge
            DotMatrixClock(
                hours = hours,
                minutes = telemetry.clock.minutes,
                pixelSize = 6.8.dp,
                pixelGap = 2.6.dp,
                digitColor = Color(0xFFF2F3F5),
                dividerColor = colors.accent
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = telemetry.clock.formattedDate,
                color = Color(0xFF9EA3AE),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp
            )

            if (notificationCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF18191C))
                        .kansoTactileClickable { shadeStage = ShadeStage.NOTIFICATIONS }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$notificationCount alerts • Pull down",
                        color = Color(0xFFE2E5EA),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Customizable Modular Lockscreen Widget Grid (rearrange-first selection UX)
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
                onMoveItemToCell = { item, col, row ->
                    viewModel.moveHomeItemToGridCell(item, col, row, 4)
                },
                onConfigureItem = { editingWidget = it },
                onRemoveItem = { viewModel.removeHomeItem(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom tactile swipe-up unlock pill
            Row(
                modifier = Modifier
                    .testTag("lockscreen_unlock_pill")
                    .kansoNeumorphic(raised = true, cornerRadius = 22.dp, surfaceColor = Color(0xFF18191C))
                    .kansoTactileClickable(onClick = smoothUnlock)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.swipe_up_to_unlock).uppercase(),
                    color = Color(0xFFEAECEF),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp
                )
            }
        }

        // Two-Stage Notification & Customizable Control Panel on Lockscreen
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
            }
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
