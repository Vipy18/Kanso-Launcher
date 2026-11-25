package com.kanso.launcher.service

import android.service.dreams.DreamService
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.data.prefs.ThemeMode
import com.kanso.launcher.ui.theme.DotMatrixClock
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.KansoTheme
import com.kanso.launcher.ui.theme.LocalKansoColors
import kotlinx.coroutines.delay

/**
 * Native Android Screensaver / Always-On Display (DreamService).
 * Pure #000000 AMOLED canvas with 60-second OLED burn-in pixel shift.
 */
class KansoDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false

        val app = application as KansoApplication
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@KansoDreamService)
            setViewTreeSavedStateRegistryOwner(this@KansoDreamService)
            setContent {
                val settings by app.preferences.settingsFlow.collectAsStateWithLifecycle(
                    initialValue = com.kanso.launcher.data.prefs.KansoSettingsState()
                )
                val telemetry by app.telemetryRepository.telemetry.collectAsStateWithLifecycle()
                val badges by KansoNotificationListener.badgeCounts.collectAsStateWithLifecycle()

                KansoTheme(
                    themeMode = ThemeMode.DARK,
                    accentColorArgb = settings.accentColorArgb,
                    useMaterialYouAccent = settings.useMaterialYouAccent
                ) {
                    AodContent(
                        hours = if (settings.use24HourClock) telemetry.clock.hours24 else telemetry.clock.hours12,
                        minutes = telemetry.clock.minutes,
                        date = telemetry.clock.formattedDate,
                        batteryPercent = telemetry.batteryPercent,
                        notificationDotCount = badges.size.coerceAtMost(5)
                    )
                }
            }
        }
        setContentView(composeView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}

@Composable
fun AodContent(
    hours: String,
    minutes: String,
    date: String,
    batteryPercent: Int,
    notificationDotCount: Int
) {
    val colors = LocalKansoColors.current
    var shiftStep by remember { mutableIntStateOf(0) }

    // Burn-in pixel shift every 60 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            shiftStep = (shiftStep + 1) % 8
        }
    }
    val offsetX = ((shiftStep % 3) - 1) * 10
    val offsetY = ((shiftStep / 3) - 1) * 14

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Minimalist accent S-curve stroke across top edge
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
        ) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.72f)
                cubicTo(
                    size.width * 0.36f, size.height * 0.72f,
                    size.width * 0.56f, size.height * 0.28f,
                    size.width, size.height * 0.28f
                )
            }
            drawPath(
                path = path,
                color = colors.accent.copy(alpha = 0.75f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(offsetX, offsetY) }
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DotMatrixClock(
                hours = hours,
                minutes = minutes,
                pixelSize = 8.dp,
                pixelGap = 3.dp,
                digitColor = Color(0xFFEAECEF),
                dividerColor = colors.accent
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = date.uppercase(),
                color = Color(0xFF9EA3AE),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                letterSpacing = 1.4.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Circular battery ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(68.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    drawArc(
                        color = Color(0xFF26282E),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = colors.accent,
                        startAngle = -90f,
                        sweepAngle = (batteryPercent / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$batteryPercent%",
                    color = Color(0xFFEAECEF),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp
                )
            }

            if (notificationDotCount > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(notificationDotCount) { idx ->
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (idx == 0) colors.accent else Color(0xFF7B808A))
                        )
                    }
                }
            }
        }
    }
}
