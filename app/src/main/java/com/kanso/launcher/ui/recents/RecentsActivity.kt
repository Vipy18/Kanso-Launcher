package com.kanso.launcher.ui.recents

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.LauncherActivity
import com.kanso.launcher.R
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.service.KansoAccessibilityService
import com.kanso.launcher.service.KansoPerformanceGovernor
import com.kanso.launcher.service.RecentsSnapshotCache
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.theme.GeistFontFamily
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.KansoTheme
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.SpaceMonoFontFamily
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.KansoAppIcon
import com.kanso.launcher.util.RomDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

class RecentsActivity : ComponentActivity() {

    companion object {
        @Volatile
        var isForeground: Boolean = false
            private set
    }

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application as KansoApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        disableOsTransitions(opening = true)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        KansoAccessibilityService.notifyRecentsOpened()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            KansoTheme(
                themeMode = settings.themeMode,
                accentColorArgb = settings.accentColorArgb,
                useMaterialYouAccent = settings.useMaterialYouAccent
            ) {
                RecentsCarouselScreen(
                    viewModel = viewModel,
                    onLaunchApp = { pkg, act ->
                        RecentsSnapshotCache.clearDismissedCutoff(this, pkg)
                        viewModel.appRepository.launchApp(pkg, act)
                        finish()
                    },
                    onGoHome = {
                        KansoAccessibilityService.suppressRedirectUntil = System.currentTimeMillis() + 600L
                        KansoAccessibilityService.notifyRecentsOpened(600L)
                        val home = Intent(this, LauncherActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            )
                        }
                        runCatching { startActivity(home) }
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        KansoAccessibilityService.notifyRecentsOpened()
    }

    override fun onPause() {
        isForeground = false
        super.onPause()
    }

    override fun onDestroy() {
        isForeground = false
        super.onDestroy()
    }

    override fun finish() {
        isForeground = false
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
}

private data class RecentTaskEntry(
    val app: InstalledApp,
    val lastUsedMs: Long
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentsCarouselScreen(
    viewModel: LauncherViewModel,
    onLaunchApp: (String, String) -> Unit,
    onGoHome: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalKansoColors.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val recentPkgs by viewModel.recentAppPackages.collectAsStateWithLifecycle()
    val overrides by viewModel.iconOverrides.collectAsStateWithLifecycle()
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val snapshotRev by RecentsSnapshotCache.revision.collectAsStateWithLifecycle()

    var resumeTick by remember { mutableIntStateOf(0) }
    var hasUsageAccess by remember { mutableStateOf(RomDetector.hasUsageStatsPermission(context)) }
    val recents = remember { mutableStateListOf<RecentTaskEntry>() }
    val pinnedPackages = remember { mutableStateListOf<String>() }
    var loaded by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        hasUsageAccess = RomDetector.hasUsageStatsPermission(context)
        resumeTick++
        onPauseOrDispose {}
    }

    LaunchedEffect(allApps, hasUsageAccess, resumeTick) {
        val fetched = withContext(Dispatchers.IO) {
            if (hasUsageAccess) {
                queryRecentTasks(context, allApps)
            } else {
                emptyList()
            }
        }
        recents.clear()
        recents.addAll(fetched)
        loaded = true
    }

    val notifByPkg = remember(activeNotifications) {
        activeNotifications.groupBy { it.packageName }
    }

    val suggestedDockApps = remember(recentPkgs, visibleApps, recents.size) {
        val byPkg = visibleApps.associateBy { it.packageName }
        val ordered = LinkedHashMap<String, InstalledApp>()
        recentPkgs.forEach { pkg -> byPkg[pkg]?.let { ordered[pkg] = it } }
        recents.forEach { entry -> ordered.putIfAbsent(entry.app.packageName, entry.app) }
        visibleApps.forEach { app ->
            if (ordered.size < 5) ordered.putIfAbsent(app.packageName, app)
        }
        ordered.values.take(5)
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { recents.size.coerceAtLeast(1) })
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            KansoPerformanceGovernor.notifyAnimationBurst(700L)
        }
    }

    val revealProgress = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
        )
    }

    val animatedDismiss: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            scope.launch {
                revealProgress.animateTo(0f, animationSpec = tween(durationMillis = 115))
                onDismiss()
            }
        }
    }

    val animatedGoHome: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            scope.launch {
                revealProgress.animateTo(0f, animationSpec = tween(durationMillis = 105))
                onGoHome()
            }
        }
    }

    val openAppInfo: (String) -> Unit = { pkg ->
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$pkg".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    val ramPct = (telemetry.ramUsageFraction * 100f).roundToInt().coerceIn(1, 99)
    val animatedRamFill by animateFloatAsState(
        targetValue = telemetry.ramUsageFraction.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 450),
        label = "recents_ram_fill"
    )

    Box(
        modifier = Modifier
            .testTag("recents_overlay_screen")
            .fillMaxSize()
            .graphicsLayer {
                alpha = revealProgress.value.coerceIn(0f, 1f)
            }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xEE0B0C0E),
                        Color(0xE60D0E10),
                        Color(0xF5090A0C)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures { animatedGoHome() }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = revealProgress.value.coerceIn(0f, 1f)
                    val s = 0.93f + 0.07f * p
                    scaleX = s
                    scaleY = s
                    translationY = (1f - p) * 38f
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top HUD Canopy: Live RAM Telemetry Gauge + CLEAR ALL + Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF1B1C1F))
                        .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.42f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "RAM: ${telemetry.ramUsedGb}G/${telemetry.ramTotalGb}G",
                        color = Color(0xFFE3E2E4),
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF343537))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedRamFill)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(colors.accent)
                        )
                    }
                    Text(
                        text = "$ramPct%",
                        color = colors.accent,
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }

                // Quick Clear Button (preserves pinned cards)
                Row(
                    modifier = Modifier
                        .testTag("recents_clear_all_btn")
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF252629))
                        .border(0.75.dp, colors.accent.copy(alpha = 0.40f), RoundedCornerShape(50))
                        .kansoTactileClickable(enabled = recents.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val removable = recents.filter { it.app.packageName !in pinnedPackages }
                            RecentsSnapshotCache.clearAllRecentTasks(
                                context = context,
                                packages = removable.map { it.app.packageName }
                            )
                            recents.removeAll(removable.toSet())
                        }
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ClearAll,
                        contentDescription = "Clear unpinned tasks",
                        tint = colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "CLEAR",
                        color = colors.accent,
                        fontFamily = SpaceMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .testTag("recents_close_btn")
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F2022))
                        .border(0.75.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                        .kansoTactileClickable(onClick = animatedGoHome),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_recents_close),
                        tint = Color(0xFFE3E2E4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 2. Center 3D Depth-Stack Horizontal Carousel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    !hasUsageAccess -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .kansoNeumorphic(raised = true, cornerRadius = 24.dp, surfaceColor = Color(0xFF1B1C1F))
                                .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                                .kansoMechaAccents(accentActive = true, showCornerBrackets = true, showLedPip = false)
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "Grant Usage Access for Multitasking",
                                color = Color(0xFFF2F4F8),
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Allows Kanso to display recent app windows and Accessibility snapshots inside the Cyber-Mecha deck.",
                                color = Color(0xFFA7AFBC),
                                fontFamily = GeistFontFamily,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .testTag("recents_grant_usage_btn")
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.accent)
                                    .kansoTactileClickable { RomDetector.openUsageAccessSettings(context) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "OPEN USAGE ACCESS",
                                    color = colors.onAccent,
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                    }

                    loaded && recents.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DECK // STANDBY",
                                color = colors.accent,
                                fontFamily = SpaceMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.4.sp
                            )
                            Text(
                                text = "All unpinned tasks cleared from Kanso memory.",
                                color = Color(0xFF9EA6B4),
                                fontFamily = GeistFontFamily,
                                fontSize = 12.sp
                            )
                        }
                    }

                    recents.isNotEmpty() -> {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 36.dp),
                            pageSpacing = 10.dp,
                            modifier = Modifier
                                .testTag("recents_horizontal_pager")
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { animatedGoHome() }
                                }
                        ) { page ->
                            val entry = recents.getOrNull(page) ?: return@HorizontalPager
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            val absOffset = abs(pageOffset).coerceIn(0f, 1.5f)
                            val scale = 1f - absOffset * 0.11f
                            val cardAlpha = (1f - absOffset * 0.30f).coerceIn(0.42f, 1f)
                            val isFocused = page == pagerState.currentPage
                            val isPinned = entry.app.packageName in pinnedPackages

                            val dismissOffsetY = remember(entry.app.componentKey) { Animatable(0f) }
                            val snapshotBitmap = remember(entry.app.packageName, snapshotRev) {
                                RecentsSnapshotCache.get(entry.app.packageName)?.asImageBitmap()
                            }
                            val latestNotif = notifByPkg[entry.app.packageName]?.firstOrNull()

                            Column(
                                modifier = Modifier
                                    .testTag("recent_card_${entry.app.packageName}")
                                    .zIndex(10f - absOffset)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.92f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {}
                                    )
                                    .graphicsLayer {
                                        cameraDistance = 16f * density
                                        rotationY = (pageOffset * -13.5f).coerceIn(-22f, 22f)
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = pageOffset * -32f
                                        alpha = (cardAlpha * (1f - (abs(dismissOffsetY.value) / 700f).coerceIn(0f, 0.85f)))
                                        translationY = dismissOffsetY.value
                                    }
                                    .kansoNeumorphic(
                                        raised = true,
                                        cornerRadius = 22.dp,
                                        surfaceColor = Color(0xFF1F2022)
                                    )
                                    .border(
                                        width = if (isFocused) 1.25.dp else 0.75.dp,
                                        color = when {
                                            isPinned -> colors.secondaryTelemetry.copy(alpha = 0.75f)
                                            isFocused -> colors.accent.copy(alpha = 0.60f)
                                            else -> Color(0xFF504533).copy(alpha = 0.30f)
                                        },
                                        shape = RoundedCornerShape(22.dp)
                                    )
                                    .kansoMechaAccents(
                                        accentActive = isFocused,
                                        showCornerBrackets = true,
                                        showLedPip = false
                                    )
                                    .pointerInput(entry.app.componentKey, isPinned) {
                                        detectVerticalDragGestures(
                                            onVerticalDrag = { change, dragAmount ->
                                                if (dragAmount < 0f || dismissOffsetY.value < 0f) {
                                                    change.consume()
                                                    val factor = if (isPinned) 0.22f else 1f
                                                    scope.launch {
                                                        dismissOffsetY.snapTo(
                                                            (dismissOffsetY.value + dragAmount * factor).coerceAtMost(20f)
                                                        )
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (!isPinned && dismissOffsetY.value < -175f) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch {
                                                        dismissOffsetY.animateTo(
                                                            targetValue = -1100f,
                                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                                        )
                                                        RecentsSnapshotCache.closeRecentTask(context, entry.app.packageName)
                                                        recents.remove(entry)
                                                    }
                                                } else {
                                                    if (isPinned && dismissOffsetY.value < -35f) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    scope.launch {
                                                        dismissOffsetY.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f)
                                                        )
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                scope.launch {
                                                    dismissOffsetY.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 420f))
                                                }
                                            }
                                        )
                                    }
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Card Header Bar: App Icon + Title + Window Controls (Pin / Info / Close)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0D0E10))
                                            .border(0.75.dp, colors.accent.copy(alpha = 0.35f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        KansoAppIcon(
                                            packageName = entry.app.packageName,
                                            activityName = entry.app.activityName,
                                            contentDescription = entry.app.label,
                                            iconPackManager = viewModel.iconPackManager,
                                            iconPackMode = settings.iconPackMode,
                                            selectedIconPackPackage = settings.selectedIconPackPackage,
                                            iconOverride = overrides[entry.app.componentKey],
                                            iconSize = 24.dp,
                                            cornerRadius = 7.dp
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.app.label.uppercase(),
                                            color = Color(0xFFEAECEF),
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                append(if (snapshotBitmap != null) "LIVE SNAP" else entry.app.subsystemTag)
                                                append(" • ")
                                                append(formatLastUsed(entry.lastUsedMs))
                                                if (isPinned) append(" • PINNED")
                                            },
                                            color = if (isPinned) colors.secondaryTelemetry else colors.accent.copy(alpha = 0.85f),
                                            fontFamily = SpaceMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            letterSpacing = 0.6.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Lock/Pin button
                                    Box(
                                        modifier = Modifier
                                            .testTag("recent_pin_${entry.app.packageName}")
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isPinned) colors.accent else Color(0xFF292A2C))
                                            .kansoTactileClickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isPinned) {
                                                    pinnedPackages.remove(entry.app.packageName)
                                                } else {
                                                    pinnedPackages.add(entry.app.packageName)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPinned) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                            contentDescription = if (isPinned) "Unpin ${entry.app.label}" else "Pin ${entry.app.label}",
                                            tint = if (isPinned) colors.onAccent else Color(0xFFD4C4AC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Close button (tap: kill & dismiss; long-press: App Info for Force Stop)
                                    Box(
                                        modifier = Modifier
                                            .testTag("recent_dismiss_${entry.app.packageName}")
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF292A2C))
                                            .kansoTactileClickable(
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    openAppInfo(entry.app.packageName)
                                                },
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    pinnedPackages.remove(entry.app.packageName)
                                                    RecentsSnapshotCache.closeRecentTask(context, entry.app.packageName)
                                                    recents.remove(entry)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close ${entry.app.label}",
                                            tint = Color(0xFFE0E4EC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                // Inner Recessed App Screen Preview
                                val innerFrameShape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(innerFrameShape)
                                        .background(Color(0xFF0D0E10))
                                        .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.32f), innerFrameShape)
                                        .kansoTactileClickable {
                                            onLaunchApp(entry.app.packageName, entry.app.activityName)
                                        }
                                ) {
                                    if (snapshotBitmap != null) {
                                        Image(
                                            bitmap = snapshotBitmap,
                                            contentDescription = "${entry.app.label} window preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFF18191C))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(colors.accent)
                                                    )
                                                    Text(
                                                        text = entry.app.subsystemTag,
                                                        color = colors.accent,
                                                        fontFamily = SpaceMonoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 8.5.sp
                                                    )
                                                }
                                                Text(
                                                    text = entry.app.packageName.substringAfterLast('.').uppercase(),
                                                    color = Color(0xFF9D8F78),
                                                    fontFamily = SpaceMonoFontFamily,
                                                    fontSize = 8.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(86.dp)
                                                        .kansoNeumorphic(raised = true, cornerRadius = 22.dp, surfaceColor = Color(0xFF1A1B1E))
                                                        .border(0.75.dp, colors.accent.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    KansoAppIcon(
                                                        packageName = entry.app.packageName,
                                                        activityName = entry.app.activityName,
                                                        contentDescription = entry.app.label,
                                                        iconPackManager = viewModel.iconPackManager,
                                                        iconPackMode = settings.iconPackMode,
                                                        selectedIconPackPackage = settings.selectedIconPackPackage,
                                                        iconOverride = overrides[entry.app.componentKey],
                                                        iconSize = 58.dp,
                                                        cornerRadius = 15.dp
                                                    )
                                                }
                                                if (latestNotif != null) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFF1E2025))
                                                            .border(0.75.dp, colors.accent.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                                            .padding(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = latestNotif.title,
                                                            color = Color(0xFFF0F3F8),
                                                            fontFamily = GeistFontFamily,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (latestNotif.text.isNotBlank()) {
                                                            Text(
                                                                text = latestNotif.text,
                                                                color = Color(0xFFA5AEBC),
                                                                fontFamily = GeistFontFamily,
                                                                fontSize = 10.sp,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    tint = colors.accent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (isPinned) "PINNED • TAP TO RESUME" else "TAP TO RESUME • SWIPE UP TO CLOSE",
                                                    color = Color(0xFF9D8F78),
                                                    fontFamily = SpaceMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.sp,
                                                    letterSpacing = 0.6.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Card Bottom Sub-Action Strip (OPEN • PIN • FORCE STOP • APP INFO)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(Color(0xFF292A2C))
                                            .kansoTactileClickable {
                                                onLaunchApp(entry.app.packageName, entry.app.activityName)
                                            },
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInNew,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "OPEN",
                                            color = Color(0xFFD4C4AC),
                                            fontFamily = SpaceMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isPinned) colors.secondaryTelemetry.copy(alpha = 0.16f) else Color(0xFF292A2C))
                                            .kansoTactileClickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isPinned) pinnedPackages.remove(entry.app.packageName)
                                                else pinnedPackages.add(entry.app.packageName)
                                            },
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isPinned) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                            contentDescription = null,
                                            tint = colors.secondaryTelemetry,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPinned) "LOCKED" else "PIN",
                                            color = if (isPinned) colors.secondaryTelemetry else Color(0xFFD4C4AC),
                                            fontFamily = SpaceMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .testTag("recent_force_stop_${entry.app.packageName}")
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(colors.alertCrimson.copy(alpha = 0.16f))
                                            .border(0.75.dp, colors.alertCrimson.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                                            .kansoTactileClickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                pinnedPackages.remove(entry.app.packageName)
                                                RecentsSnapshotCache.closeRecentTask(context, entry.app.packageName)
                                                recents.remove(entry)
                                                val accessibilityRunning = KansoAccessibilityService.isRunning.value
                                                if (accessibilityRunning) {
                                                    KansoAccessibilityService.requestAssistedForceStop(entry.app.packageName)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Tap Force Stop in App Info to terminate foreground services",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                openAppInfo(entry.app.packageName)
                                            }
                                            .padding(horizontal = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PowerSettingsNew,
                                            contentDescription = "Force stop ${entry.app.label}",
                                            tint = colors.alertCrimson,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .testTag("recents_app_info_btn")
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(Color(0xFF292A2C))
                                            .kansoTactileClickable { openAppInfo(entry.app.packageName) }
                                            .padding(horizontal = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = "App info for ${entry.app.label}",
                                            tint = Color(0xFF9D8F78),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bottom Overview Action Bar + 5-Icon Suggested Quick Dock + Telemetry Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .testTag("recents_screenshot_btn")
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1F2022))
                            .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.40f), RoundedCornerShape(50))
                            .kansoTactileClickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val ok = KansoAccessibilityService.performTakeScreenshot()
                                if (!ok) {
                                    Toast.makeText(
                                        context,
                                        "Enable Kanso Accessibility Service for screenshots",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                }
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Screenshot,
                            contentDescription = "Take screenshot",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SCREENSHOT",
                            color = Color(0xFFE3E2E4),
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(
                        modifier = Modifier
                            .testTag("recents_home_btn")
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1F2022))
                            .border(0.75.dp, colors.accent.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .kansoTactileClickable(onClick = animatedGoHome),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Home,
                            contentDescription = "Go Home",
                            tint = colors.secondaryTelemetry,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOME DECK",
                            color = Color(0xFFE3E2E4),
                            fontFamily = SpaceMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // 5-Icon Suggested Quick Launch Dock
                if (suggestedDockApps.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1A1B1E).copy(alpha = 0.92f))
                            .border(0.75.dp, Color(0xFF504533).copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        suggestedDockApps.forEachIndexed { idx, app ->
                            Column(
                                modifier = Modifier
                                    .testTag("recents_dock_${app.packageName}")
                                    .width(56.dp)
                                    .kansoTactileClickable {
                                        onLaunchApp(app.packageName, app.activityName)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0E0F12))
                                        .border(
                                            width = if (idx == 0) 1.dp else 0.75.dp,
                                            color = if (idx == 0) colors.accent.copy(alpha = 0.75f) else Color(0xFF504533).copy(alpha = 0.35f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    KansoAppIcon(
                                        packageName = app.packageName,
                                        activityName = app.activityName,
                                        contentDescription = app.label,
                                        iconPackManager = viewModel.iconPackManager,
                                        iconPackMode = settings.iconPackMode,
                                        selectedIconPackPackage = settings.selectedIconPackPackage,
                                        iconOverride = overrides[app.componentKey],
                                        iconSize = 24.dp,
                                        cornerRadius = 7.dp
                                    )
                                }
                                Text(
                                    text = app.label.uppercase(),
                                    color = if (idx == 0) colors.accent else Color(0xFF9D8F78),
                                    fontFamily = SpaceMonoFontFamily,
                                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 7.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Monospace Telemetry Footer
                Text(
                    text = "RECENTS // ${recents.size} ACTIVE APPS // KANSO OS v4.2",
                    color = Color(0xFF9D8F78),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 1.2.sp
                )

                // Home Indicator Pill
                Box(
                    modifier = Modifier
                        .size(width = 112.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF343537))
                )
            }
        }
    }
}

private fun queryRecentTasks(context: Context, allApps: List<InstalledApp>): List<RecentTaskEntry> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
    val now = System.currentTimeMillis()
    val stats = runCatching {
        usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 24 * 60 * 60 * 1000L, now)
    }.getOrNull().orEmpty()

    val dismissedCutoffs = RecentsSnapshotCache.getDismissedCutoffs(context)
    val appByPkg = allApps.associateBy { it.packageName }
    val seen = HashSet<String>()
    return stats
        .filter { stat ->
            stat.packageName != context.packageName &&
                RecentsSnapshotCache.shouldIncludeTask(stat.packageName, stat.lastTimeUsed, dismissedCutoffs)
        }
        .sortedByDescending { it.lastTimeUsed }
        .mapNotNull { stat ->
            if (!seen.add(stat.packageName)) return@mapNotNull null
            val app = appByPkg[stat.packageName] ?: return@mapNotNull null
            RecentTaskEntry(app = app, lastUsedMs = stat.lastTimeUsed)
        }
        .take(14)
}

private fun formatLastUsed(lastUsedMs: Long): String {
    val diffMin = ((System.currentTimeMillis() - lastUsedMs) / 60000L).coerceAtLeast(0L)
    return when {
        diffMin < 1L -> "NOW"
        diffMin < 60L -> "${diffMin}M AGO"
        else -> "${diffMin / 60L}H AGO"
    }
}
