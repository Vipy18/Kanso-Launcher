package com.kanso.launcher.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.R
import com.kanso.launcher.data.prefs.ThemeMode

data class AccentPreset(
    val name: String,
    val argb: Long
)

object KansoAccents {
    const val DEFAULT_ACCENT_ARGB: Long = 0xFFF5B400L

    val Presets: List<AccentPreset> = listOf(
        AccentPreset("Signal Amber", 0xFFF5B400L),
        AccentPreset("Signal Orange", 0xFFFF4500L),
        AccentPreset("Mecha Crimson", 0xFFFF3B30L),
        AccentPreset("Cyber Cyan", 0xFF00E5FFL),
        AccentPreset("Acid Lime", 0xFF84CC16L),
        AccentPreset("Cobalt Steel", 0xFF2563EBL)
    )

    fun computeOnAccent(accent: Color): Color {
        val lum = accent.luminance()
        val contrastBlack = (lum + 0.05f) / (0.004f + 0.05f)
        val contrastWhite = (1.0f + 0.05f) / (lum + 0.05f)
        return if (contrastBlack >= contrastWhite) Color(0xFF1A1404) else Color(0xFFFFFFFF)
    }
}

/**
 * Extracts live Android 12+ Material You wallpaper accent swatches (Primary, Secondary, Tertiary)
 * so the user can sync Kanso's accent palette directly with Material You.
 */
@Composable
fun rememberMaterialYouSwatches(isDark: Boolean): List<AccentPreset> {
    val context = LocalContext.current
    return remember(context, isDark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            listOf(
                AccentPreset("Material Primary", scheme.primary.toArgb().toLong() and 0xFFFFFFFFL),
                AccentPreset("Material Secondary", scheme.secondary.toArgb().toLong() and 0xFFFFFFFFL),
                AccentPreset("Material Tertiary", scheme.tertiary.toArgb().toLong() and 0xFFFFFFFFL)
            )
        } else {
            emptyList()
        }
    }
}

@Immutable
data class KansoColors(
    val isDark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val canvasBg: Color,
    val surfaceRaised: Color,
    val surfaceRaisedHighlight: Color,
    val surfaceInset: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val shadowLightBlur: Dp,
    val shadowDarkBlur: Dp,
    val shadowLightOffset: Dp,
    val shadowDarkOffset: Dp,
    val darkModuleBg: Color,
    val darkModuleElevated: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val trackMuted: Color,
    val statusHealthy: Color,
    val statusWarning: Color,
    val secondaryTelemetry: Color = Color(0xFF00E5FF),
    val alertCrimson: Color = Color(0xFFFF3B30)
)

private fun lightKansoColors(accent: Color): KansoColors = KansoColors(
    isDark = false,
    accent = accent,
    onAccent = KansoAccents.computeOnAccent(accent),
    canvasBg = Color(0xFFEAECEF),
    surfaceRaised = Color(0xFFF2F4F7),
    surfaceRaisedHighlight = Color(0xFFFAFBFC),
    surfaceInset = Color(0xFFE0E3E9),
    shadowLight = Color(0xF2FFFFFF),
    shadowDark = Color(0xE6BCC2CD),
    shadowLightBlur = 12.dp,
    shadowDarkBlur = 15.dp,
    shadowLightOffset = 5.dp,
    shadowDarkOffset = 6.dp,
    darkModuleBg = Color(0xFF1A1B1E),
    darkModuleElevated = Color(0xFF27292F),
    inkPrimary = Color(0xFF111112),
    inkSecondary = Color(0xFF6B7078),
    trackMuted = Color(0xFFD0D5DD),
    statusHealthy = Color(0xFF16A34A),
    statusWarning = Color(0xFFEA580C),
    secondaryTelemetry = Color(0xFF00B8D9),
    alertCrimson = Color(0xFFE62E2D)
)

private fun darkKansoColors(accent: Color): KansoColors = KansoColors(
    isDark = true,
    accent = accent,
    onAccent = KansoAccents.computeOnAccent(accent),
    canvasBg = Color(0xFF121315),
    surfaceRaised = Color(0xFF1F2022),
    surfaceRaisedHighlight = Color(0xFF292A2C),
    surfaceInset = Color(0xFF0D0E10),
    shadowLight = Color(0x9934363D),
    shadowDark = Color(0xF2030405),
    shadowLightBlur = 10.dp,
    shadowDarkBlur = 16.dp,
    shadowLightOffset = 4.dp,
    shadowDarkOffset = 6.5.dp,
    darkModuleBg = Color(0xFF181A1D),
    darkModuleElevated = Color(0xFF24262B),
    inkPrimary = Color(0xFFE3E2E4),
    inkSecondary = Color(0xFF8A909A),
    trackMuted = Color(0xFF2D2F35),
    statusHealthy = Color(0xFF22C55E),
    statusWarning = Color(0xFFF5B400),
    secondaryTelemetry = Color(0xFF00E5FF),
    alertCrimson = Color(0xFFFF3B30)
)

val LocalKansoColors = staticCompositionLocalOf {
    darkKansoColors(Color(KansoAccents.DEFAULT_ACCENT_ARGB))
}

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val GeistFontFamily = FontFamily(
    Font(R.font.geist, FontWeight.Normal),
    Font(R.font.geist, FontWeight.Medium),
    Font(R.font.geist, FontWeight.SemiBold),
    Font(R.font.geist, FontWeight.Bold)
)

val SpaceMonoFontFamily = FontFamily(
    Font(R.font.space_mono, FontWeight.Normal),
    Font(R.font.space_mono, FontWeight.Medium),
    Font(R.font.space_mono, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = SpaceMonoFontFamily

val KansoTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GeistFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SpaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 8.5.sp,
        lineHeight = 11.sp,
        letterSpacing = 1.0.sp
    )
)

val KansoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun KansoTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColorArgb: Long = KansoAccents.DEFAULT_ACCENT_ARGB,
    useMaterialYouAccent: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val resolvedAccent = if (useMaterialYouAccent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynScheme = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dynScheme.primary
    } else {
        Color(accentColorArgb.toInt())
    }

    val targetPalette = if (useDark) darkKansoColors(resolvedAccent) else lightKansoColors(resolvedAccent)

    val animSpec = spring<Color>(stiffness = 380f)
    val accent by animateColorAsState(targetPalette.accent, animSpec, label = "kansoAccent")
    val onAccent by animateColorAsState(targetPalette.onAccent, animSpec, label = "kansoOnAccent")
    val canvasBg by animateColorAsState(targetPalette.canvasBg, animSpec, label = "kansoCanvas")
    val surfaceRaised by animateColorAsState(targetPalette.surfaceRaised, animSpec, label = "kansoRaised")
    val surfaceRaisedHighlight by animateColorAsState(targetPalette.surfaceRaisedHighlight, animSpec, label = "kansoRaisedHi")
    val surfaceInset by animateColorAsState(targetPalette.surfaceInset, animSpec, label = "kansoInset")
    val shadowLight by animateColorAsState(targetPalette.shadowLight, animSpec, label = "kansoShadowLight")
    val shadowDark by animateColorAsState(targetPalette.shadowDark, animSpec, label = "kansoShadowDark")
    val darkModuleBg by animateColorAsState(targetPalette.darkModuleBg, animSpec, label = "kansoDarkModule")
    val darkModuleElevated by animateColorAsState(targetPalette.darkModuleElevated, animSpec, label = "kansoDarkModuleElev")
    val inkPrimary by animateColorAsState(targetPalette.inkPrimary, animSpec, label = "kansoInkPrimary")
    val inkSecondary by animateColorAsState(targetPalette.inkSecondary, animSpec, label = "kansoInkSecondary")
    val trackMuted by animateColorAsState(targetPalette.trackMuted, animSpec, label = "kansoTrackMuted")

    val animatedColors = targetPalette.copy(
        accent = accent,
        onAccent = onAccent,
        canvasBg = canvasBg,
        surfaceRaised = surfaceRaised,
        surfaceRaisedHighlight = surfaceRaisedHighlight,
        surfaceInset = surfaceInset,
        shadowLight = shadowLight,
        shadowDark = shadowDark,
        darkModuleBg = darkModuleBg,
        darkModuleElevated = darkModuleElevated,
        inkPrimary = inkPrimary,
        inkSecondary = inkSecondary,
        trackMuted = trackMuted
    )

    val materialScheme = if (useDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = canvasBg,
            onBackground = inkPrimary,
            surface = surfaceRaised,
            onSurface = inkPrimary,
            surfaceVariant = surfaceInset,
            onSurfaceVariant = inkSecondary,
            outline = trackMuted
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            background = canvasBg,
            onBackground = inkPrimary,
            surface = surfaceRaised,
            onSurface = inkPrimary,
            surfaceVariant = surfaceInset,
            onSurfaceVariant = inkSecondary,
            outline = trackMuted
        )
    }

    CompositionLocalProvider(LocalKansoColors provides animatedColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = KansoTypography,
            shapes = KansoShapes,
            content = content
        )
    }
}
