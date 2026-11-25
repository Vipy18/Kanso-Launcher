package com.kanso.launcher.ui.settings

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.ui.theme.ColorBlockHeaderBackdrop
import com.kanso.launcher.ui.theme.DotMatrixClock
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.KansoAccents
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.rememberMaterialYouSwatches

@Composable
fun AccentColorPickerSection(
    currentAccentArgb: Long,
    useMaterialYouAccent: Boolean,
    onToggleMaterialYou: (Boolean) -> Unit,
    onAccentSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val materialYouSwatches = rememberMaterialYouSwatches(colors.isDark)

    val initialHsv = remember(currentAccentArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(currentAccentArgb.toInt(), it) }
    }

    var hue by remember(currentAccentArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(currentAccentArgb) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(currentAccentArgb) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember(currentAccentArgb) {
        mutableStateOf(String.format("#%06X", (currentAccentArgb.toInt() and 0xFFFFFF)))
    }
    var showCustomPicker by remember { mutableStateOf(false) }

    val previewColor = if (useMaterialYouAccent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colors.accent
    } else {
        remember(hue, saturation, value) {
            Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)))
        }
    }
    val previewOnAccent = remember(previewColor) { KansoAccents.computeOnAccent(previewColor) }
    val previewHex = remember(previewColor) {
        String.format("#%06X", previewColor.toArgb() and 0xFFFFFF)
    }

    LaunchedEffect(currentAccentArgb) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(currentAccentArgb.toInt(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexInput = String.format("#%06X", (currentAccentArgb.toInt() and 0xFFFFFF))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live S-curve mini preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.canvasBg)
        ) {
            ColorBlockHeaderBackdrop(
                height = 76.dp,
                leftDropFraction = 0.96f,
                rightShelfFraction = 0.66f,
                curveCenterXFraction = 0.50f,
                accentColor = previewColor
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DotMatrixClock(
                    hours = "21",
                    minutes = "03",
                    pixelSize = 2.6.dp,
                    pixelGap = 1.dp,
                    digitColor = previewOnAccent,
                    dividerColor = previewOnAccent
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.darkModuleBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (useMaterialYouAccent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "MAT-YOU $previewHex"
                        } else {
                            previewHex
                        },
                        color = previewColor,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
            }
        }

        // Android 12+ Material You Wallpaper Accent Sync
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "Sync with Android Material You",
                            color = colors.inkPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Match wallpaper dynamic accent color automatically",
                            color = colors.inkSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = useMaterialYouAccent,
                    onCheckedChange = onToggleMaterialYou,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onAccent,
                        checkedTrackColor = colors.accent
                    ),
                    modifier = Modifier.testTag("toggle_material_you_accent")
                )
            }

            if (materialYouSwatches.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WALLPAPER",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                    materialYouSwatches.forEach { swatch ->
                        val swatchColor = Color(swatch.argb.toInt())
                        val selected = !useMaterialYouAccent &&
                            (currentAccentArgb and 0xFFFFFFL) == (swatch.argb and 0xFFFFFFL)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .then(
                                    if (selected) Modifier.border(2.dp, colors.inkPrimary, CircleShape)
                                    else Modifier
                                )
                                .kansoTactileClickable {
                                    onToggleMaterialYou(false)
                                    onAccentSelected(swatch.argb)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = swatch.name,
                                    tint = KansoAccents.computeOnAccent(swatchColor),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6 Curated Industrial Preset Swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KansoAccents.Presets.forEach { preset ->
                val selected = !useMaterialYouAccent &&
                    (currentAccentArgb and 0xFFFFFFL) == (preset.argb and 0xFFFFFFL)
                val swatchColor = Color(preset.argb.toInt())
                Box(
                    modifier = Modifier
                        .testTag("accent_preset_${preset.name.lowercase().replace(' ', '_')}")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .then(
                            if (selected) Modifier.border(2.5.dp, colors.inkPrimary, CircleShape)
                            else Modifier
                        )
                        .kansoTactileClickable {
                            if (useMaterialYouAccent) onToggleMaterialYou(false)
                            onAccentSelected(preset.argb)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = preset.name,
                            tint = KansoAccents.computeOnAccent(swatchColor),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Toggle Custom HSV / HEX Picker
        Row(
            modifier = Modifier
                .testTag("toggle_custom_color_picker")
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
                .kansoTactileClickable { showCustomPicker = !showCustomPicker }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showCustomPicker) "Hide Custom HSV / HEX Picker" else "Custom HSV / HEX Color Picker",
                color = colors.inkPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(previewColor)
            )
        }

        if (showCustomPicker) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HsvSliderRow(
                    label = "HUE ${hue.toInt()}°",
                    value = hue,
                    range = 0f..360f,
                    accent = previewColor,
                    onValueChange = {
                        if (useMaterialYouAccent) onToggleMaterialYou(false)
                        hue = it
                        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
                        hexInput = String.format("#%06X", argb and 0xFFFFFF)
                        onAccentSelected(argb.toLong() and 0xFFFFFFFFL)
                    }
                )
                HsvSliderRow(
                    label = "SAT ${(saturation * 100).toInt()}%",
                    value = saturation,
                    range = 0.15f..1f,
                    accent = previewColor,
                    onValueChange = {
                        if (useMaterialYouAccent) onToggleMaterialYou(false)
                        saturation = it
                        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
                        hexInput = String.format("#%06X", argb and 0xFFFFFF)
                        onAccentSelected(argb.toLong() and 0xFFFFFFFFL)
                    }
                )
                HsvSliderRow(
                    label = "VAL ${(value * 100).toInt()}%",
                    value = value,
                    range = 0.25f..1f,
                    accent = previewColor,
                    onValueChange = {
                        if (useMaterialYouAccent) onToggleMaterialYou(false)
                        value = it
                        val argb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
                        hexInput = String.format("#%06X", argb and 0xFFFFFF)
                        onAccentSelected(argb.toLong() and 0xFFFFFFFFL)
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { raw ->
                            hexInput = raw.take(7)
                            val clean = raw.removePrefix("#")
                            if (clean.length == 6) {
                                runCatching { "#$clean".toColorInt() }.getOrNull()?.let { parsed ->
                                    if (useMaterialYouAccent) onToggleMaterialYou(false)
                                    onAccentSelected(parsed.toLong() and 0xFFFFFFFFL)
                                }
                            }
                        },
                        singleLine = true,
                        label = { Text("HEX (#RRGGBB)") },
                        modifier = Modifier
                            .testTag("custom_hex_input")
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HsvSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalKansoColors.current
    Column {
        Text(
            text = label,
            color = colors.inkSecondary,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent
            ),
            modifier = Modifier.height(30.dp)
        )
    }
}
