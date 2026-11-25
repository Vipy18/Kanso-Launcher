package com.kanso.launcher.ui.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.R
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis

/**
 * Classic 12-lobe Scalloped tactile Weather Medallion (DEFAULT) or optional
 * machined Mecha weather sensor pod when a Mecha theme is selected.
 */
@Composable
fun WeatherBadgeWidget(
    weather: CachedWeather,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current

        BoxWithConstraints(
            modifier = modifier.testTag("widget_weather_badge"),
            contentAlignment = Alignment.Center
        ) {
            val defaultSize = if (mechaTheme == MechaWidgetTheme.DEFAULT) 138.dp else 120.dp
            val targetSize = if (maxWidth.value.isFinite() && maxHeight.value.isFinite() && maxWidth > 48.dp && maxHeight > 48.dp) {
                minOf(maxWidth, maxHeight).coerceIn(92.dp, 210.dp)
            } else {
                defaultSize
            }
            val scale = (targetSize / 136.dp).coerceIn(0.72f, 1.45f)

            if (mechaTheme == MechaWidgetTheme.DEFAULT) {
                val scallopedShape = remember { ScallopedShape(lobes = 12, depthRatio = 0.068f) }
                Box(
                    modifier = Modifier
                        .size(targetSize)
                        .kansoNeumorphic(
                            raised = true,
                            customShape = scallopedShape
                        )
                        .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick)
                        .padding(horizontal = (14f * scale).dp, vertical = (12f * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (weather.isUsingAutoLocation) {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size((9.5f * scale).dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = weather.cityName.uppercase(),
                                color = colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = (7.4f * scale).sp,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height((5f * scale).dp))

                        Text(
                            text = "Weather ${weather.temperatureC}°",
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = (13.5f * scale).sp,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height((6f * scale).dp))

                        AnimatedContent(
                            targetState = weatherIconForWmoCode(weather.weatherCode),
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "weatherIconMorphClassic"
                        ) { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = weather.cityName,
                                tint = colors.inkPrimary,
                                modifier = Modifier.size((32f * scale).dp)
                            )
                        }

                        Spacer(modifier = Modifier.height((7f * scale).dp))

                        Text(
                            text = stringResource(R.string.humidity_label, weather.humidityPercent),
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = (7.4f * scale).sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }
                return@BoxWithConstraints
            }

            Box(
                modifier = Modifier
                    .size(targetSize)
                    .mechaMetallicChassis(
                        theme = mechaTheme,
                        darkPlate = false,
                        accentActive = weather.isUsingAutoLocation,
                        cutDp = (14f * scale).dp
                    )
                    .kansoTactileClickable(onLongClick = onLongClick, onClick = onClick)
                    .padding((10f * scale).dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (weather.isUsingAutoLocation) {
                            Icon(
                                imageVector = Icons.Rounded.MyLocation,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size((9.5f * scale).dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = weather.cityName.uppercase(),
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (7.8f * scale).sp,
                            letterSpacing = 0.6.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height((3f * scale).dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((5f * scale).dp)
                    ) {
                        AnimatedContent(
                            targetState = weatherIconForWmoCode(weather.weatherCode),
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "weatherIconMorph"
                        ) { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = weather.cityName,
                                tint = colors.accent,
                                modifier = Modifier.size((24f * scale).dp)
                            )
                        }
                        Text(
                            text = "${weather.temperatureC}°",
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (22f * scale).sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height((5f * scale).dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(1.dp)
                            .background(colors.inkSecondary.copy(alpha = 0.28f))
                    )

                    Spacer(modifier = Modifier.height((4f * scale).dp))

                    Text(
                        text = stringResource(R.string.humidity_label, weather.humidityPercent),
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = (7.5f * scale).sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun weatherIconForWmoCode(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Rounded.WbSunny
    2, 3, 45, 48 -> Icons.Rounded.Cloud
    in 51..67, in 80..82 -> Icons.Rounded.Umbrella
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Umbrella
}
