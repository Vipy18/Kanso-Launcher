package com.kanso.launcher.ui.recents

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.LauncherActivity
import com.kanso.launcher.R
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.service.KansoPerformanceGovernor
import com.kanso.launcher.service.RecentsSnapshotCache
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.KansoTheme
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.KansoAppIcon
import com.kanso.launcher.util.RomDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Android-style Horizontal Multitasking / Recents Window Switcher in Kanso's sharp metallic style.
 * Displays Android 11+ Accessibility window snapshots when cached, with live mecha window fallbacks,
 * spring perspective paging, and swipe-up-to-dismiss gestures.
 */
class RecentsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory(application as KansoApplication)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        disableOsTransitions(opening = true)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                        viewModel.appRepository.launchApp(pkg, act)
                        finish()
                    },
                    onGoHome = {
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
    val allApps by viewModel.allInstalledApps.collectAsStateWithLifecycle()
    val overrides by viewModel.iconOverrides.collectAsStateWithLifecycle()
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val snapshotRev by RecentsSnapshotCache.revision.collectAsStateWithLifecycle()

    val hasUsageAccess = remember { RomDetector.hasUsageStatsPermission(context) }
    val recents = remember { mutableStateListOf<RecentTaskEntry>() }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(allApps, hasUsageAccess) {
        val fetched = withContext(Dispatchers.IO) {
            if (hasUsageAccess) {
                queryRecentTasks(context, allApps)
            } else {
                // Show top apps fallback if Usage Access isn't granted yet alongside permission prompt
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
            animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMedium)
        )
    }

    val animatedDismiss: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            scope.launch {
                revealProgress.animateTo(0f, animationSpec = tween(durationMillis = 120))
                onDismiss()
            }
        }
    }

    val animatedGoHome: () -> Unit = {
        if (!isClosing) {
            isClosing = true
            scope.launch {
                revealProgress.animateTo(0f, animationSpec = tween(durationMillis = 110))
                onGoHome()
            }
        }
    }

    val buttonShape = RoundedCornerShape(14.dp)

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
                        Color(0xE80A0C10),
                        Color(0xDC0E1015),
                        Color(0xF0080A0D)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = animatedDismiss
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = revealProgress.value.coerceIn(0f, 1f)
                    val s = 0.92f + 0.08f * p
                    scaleX = s
                    scaleY = s
                    translationY = (1f - p) * 44f
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top metallic HUD header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MULTITASK // DECK",
                        color = Color(0xFFF2F4F8),
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = if (hasUsageAccess) {
                            "${recents.size} ACTIVE WINDOWS • SWIPE UP TO DISMISS"
                        } else {
                            "USAGE ACCESS REQUIRED FOR LIVE TASKS"
                        },
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.9.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .testTag("recents_close_btn")
                        .size(44.dp)
                        .kansoNeumorphic(raised = true, cornerRadius = 14.dp, surfaceColor = colors.darkModuleBg)
                        .kansoTactileClickable(onClick = animatedDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_recents_close),
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Center Window Carousel or Usage Access Prompt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    !hasUsageAccess -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .kansoNeumorphic(raised = true, cornerRadius = 24.dp, surfaceColor = colors.darkModuleBg)
                                .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
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
                                text = "Allows Kanso to display recent app windows and Accessibility snapshots inside the launcher-styled switcher.",
                                color = Color(0xFFA7AFBC),
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .testTag("recents_grant_usage_btn")
                                    .clip(buttonShape)
                                    .background(colors.accent)
                                    .kansoTactileClickable { RomDetector.openUsageAccessSettings(context) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "OPEN USAGE ACCESS",
                                    color = colors.onAccent,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
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
                                text = "NO RECENT WINDOWS",
                                color = Color(0xFFE6EAF0),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "All tasks cleared from Kanso deck.",
                                color = Color(0xFF9EA6B4),
                                fontSize = 12.sp
                            )
                        }
                    }

                    recents.isNotEmpty() -> {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 42.dp),
                            pageSpacing = 10.dp,
                            modifier = Modifier
                                .testTag("recents_horizontal_pager")
                                .fillMaxSize()
                        ) { page ->
                            val entry = recents.getOrNull(page) ?: return@HorizontalPager
                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            val absOffset = abs(pageOffset).coerceIn(0f, 1.5f)
                            val scale = 1f - absOffset * 0.11f
                            val cardAlpha = (1f - absOffset * 0.24f).coerceIn(0.45f, 1f)

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
                                    .fillMaxHeight(0.84f)
                                    .graphicsLayer {
                                        cameraDistance = 16f * density
                                        rotationY = (pageOffset * -13.5f).coerceIn(-22f, 22f)
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = pageOffset * -34f
                                        alpha = (cardAlpha * (1f - (abs(dismissOffsetY.value) / 700f).coerceIn(0f, 0.85f)))
                                        translationY = dismissOffsetY.value
                                    }
                                    .pointerInput(entry.app.componentKey) {
                                        detectVerticalDragGestures(
                                            onVerticalDrag = { change, dragAmount ->
                                                if (dragAmount < 0f || dismissOffsetY.value < 0f) {
                                                    change.consume()
                                                    scope.launch {
                                                        dismissOffsetY.snapTo((dismissOffsetY.value + dragAmount).coerceAtMost(24f))
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (dismissOffsetY.value < -180f) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch {
                                                        dismissOffsetY.animateTo(
                                                            targetValue = -1100f,
                                                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                                        )
                                                        RecentsSnapshotCache.remove(entry.app.packageName)
                                                        recents.remove(entry)
                                                    }
                                                } else {
                                                    scope.launch {
                                                        dismissOffsetY.animateTo(
                                                            targetValue = 0f,
                                                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f)
                                                        )
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                scope.launch {
                                                    dismissOffsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 420f))
                                                }
                                            }
                                        )
                                    },
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Window Header Bar (Icon + Title + Dismiss X)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    KansoAppIcon(
                                        packageName = entry.app.packageName,
                                        activityName = entry.app.activityName,
                                        contentDescription = entry.app.label,
                                        iconPackManager = viewModel.iconPackManager,
                                        iconPackMode = settings.iconPackMode,
                                        selectedIconPackPackage = settings.selectedIconPackPackage,
                                        iconOverride = overrides[entry.app.componentKey],
                                        iconSize = 38.dp,
                                        cornerRadius = 10.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.app.label,
                                            color = Color(0xFFF4F6FA),
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (snapshotBitmap != null) "LIVE SNAPSHOT" else "ACTIVE TASK",
                                            color = colors.accent,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.5.sp,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .testTag("recent_dismiss_${entry.app.packageName}")
                                            .size(34.dp)
                                            .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.55f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(Color(0xFF1F232B))
                                            .kansoTactileClickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                RecentsSnapshotCache.remove(entry.app.packageName)
                                                recents.remove(entry)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss ${entry.app.label}",
                                            tint = Color(0xFFE0E4EC),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Window Preview Frame (Rounded Neumorphic Shell + Snapshot or Live App Fallback)
                                val innerFrameShape = RoundedCornerShape(17.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .kansoNeumorphic(
                                            raised = true,
                                            cornerRadius = 22.dp,
                                            surfaceColor = colors.darkModuleBg
                                        )
                                        .border(
                                            width = if (page == pagerState.currentPage) 1.2.dp else 0.7.dp,
                                            color = if (page == pagerState.currentPage) colors.accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .kansoTactileClickable {
                                            onLaunchApp(entry.app.packageName, entry.app.activityName)
                                        }
                                        .padding(6.dp)
                                ) {
                                    if (snapshotBitmap != null) {
                                        Image(
                                            bitmap = snapshotBitmap,
                                            contentDescription = "${entry.app.label} window preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(innerFrameShape)
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(innerFrameShape)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFF171A21), Color(0xFF0E1015))
                                                    )
                                                )
                                                .border(
                                                    width = 0.8.dp,
                                                    color = colors.accent.copy(alpha = 0.22f),
                                                    shape = innerFrameShape
                                                )
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = entry.app.packageName.substringAfterLast('.').uppercase(),
                                                    color = Color(0xFF8C94A3),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 1.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 22.dp, height = 4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(colors.accent)
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                KansoAppIcon(
                                                    packageName = entry.app.packageName,
                                                    activityName = entry.app.activityName,
                                                    contentDescription = entry.app.label,
                                                    iconPackManager = viewModel.iconPackManager,
                                                    iconPackMode = settings.iconPackMode,
                                                    selectedIconPackPackage = settings.selectedIconPackPackage,
                                                    iconOverride = overrides[entry.app.componentKey],
                                                    iconSize = 78.dp,
                                                    cornerRadius = 18.dp
                                                )
                                                if (latestNotif != null) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color(0xFF1E222B))
                                                            .padding(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = latestNotif.title,
                                                            color = Color(0xFFF0F3F8),
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.5.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (latestNotif.text.isNotBlank()) {
                                                            Text(
                                                                text = latestNotif.text,
                                                                color = Color(0xFFA0A8B8),
                                                                fontSize = 10.5.sp,
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
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "TAP TO RESUME • SWIPE UP TO CLOSE",
                                                    color = Color(0xFFA5AEBC),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.5.sp,
                                                    letterSpacing = 0.7.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Metallic Multitasking Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val focusedEntry = recents.getOrNull(pagerState.currentPage)
                Box(
                    modifier = Modifier
                        .testTag("recents_clear_all_btn")
                        .weight(1f)
                        .height(48.dp)
                        .kansoNeumorphic(raised = true, cornerRadius = 15.dp, surfaceColor = colors.darkModuleBg)
                        .kansoTactileClickable(enabled = recents.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            RecentsSnapshotCache.clear()
                            recents.clear()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ClearAll,
                            contentDescription = "Clear all tasks",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CLEAR ALL",
                            color = Color(0xFFF2F4F8),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                if (focusedEntry != null) {
                    Box(
                        modifier = Modifier
                            .testTag("recents_app_info_btn")
                            .size(48.dp)
                            .kansoNeumorphic(raised = true, cornerRadius = 15.dp, surfaceColor = colors.darkModuleBg)
                            .kansoTactileClickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = "package:${focusedEntry.app.packageName}".toUri()
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "App info",
                            tint = Color(0xFFE6EAF0),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .testTag("recents_home_btn")
                        .size(48.dp)
                        .kansoIconNeumorphic(cornerRadius = 14.dp, shadowScale = 0.65f)
                        .clip(buttonShape)
                        .background(colors.accent)
                        .kansoTactileClickable(onClick = animatedGoHome),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "Go Home",
                        tint = colors.onAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
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

    val appByPkg = allApps.associateBy { it.packageName }
    val seen = HashSet<String>()
    return stats
        .filter { it.lastTimeUsed > 0L && it.packageName != context.packageName }
        .sortedByDescending { it.lastTimeUsed }
        .mapNotNull { stat ->
            if (!seen.add(stat.packageName)) return@mapNotNull null
            val app = appByPkg[stat.packageName] ?: return@mapNotNull null
            RecentTaskEntry(app = app, lastUsedMs = stat.lastTimeUsed)
        }
        .take(14)
}
