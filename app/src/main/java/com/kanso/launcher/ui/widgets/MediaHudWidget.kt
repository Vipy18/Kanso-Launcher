package com.kanso.launcher.ui.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kanso.launcher.R
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis

/**
 * Sharp machined metallic Media Deck HUD strictly using active accent + launcher defaults.
 */
@Composable
fun MediaHudWidget(
    media: MediaHudState,
    notificationAccessGranted: Boolean = true,
    mechaTheme: MechaWidgetTheme = MechaWidgetTheme.DEFAULT,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onRequestMediaPermission: (() -> Unit)? = null,
    onOpenPlayerApp: ((String) -> Unit)? = null,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    WithMechaWidgetTheme(mechaTheme) {
        val colors = LocalKansoColors.current
        val artBitmap = remember(media.albumArt) { media.albumArt?.asImageBitmap() }
        val artRadius = if (mechaTheme == MechaWidgetTheme.DEFAULT) 14.dp else 7.dp
        val btnRadius = if (mechaTheme == MechaWidgetTheme.DEFAULT) 9.dp else 5.dp
        val artShape = RoundedCornerShape(artRadius)
        val btnShape = RoundedCornerShape(btnRadius)

        Row(
            modifier = modifier
                .testTag("widget_media_hud")
                .height(88.dp)
                .mechaMetallicChassis(
                    theme = mechaTheme,
                    darkPlate = false,
                    accentActive = media.isPlaying,
                    cutDp = 12.dp
                )
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .testTag("media_hud_art_play_pause")
                    .size(72.dp)
                    .kansoIconNeumorphic(cornerRadius = artRadius, shadowScale = 0.55f)
                    .clip(artShape)
                    .background(Brush.linearGradient(listOf(colors.accent, colors.darkModuleBg)))
                    .border(1.dp, colors.accent.copy(alpha = 0.55f), artShape)
                    .kansoTactileClickable(
                        onLongClick = onLongClick,
                        onClick = {
                            if (!notificationAccessGranted && !media.hasActiveSession && onRequestMediaPermission != null) {
                                onRequestMediaPermission()
                            } else if (media.packageName.isNotBlank() && onOpenPlayerApp != null) {
                                onOpenPlayerApp(media.packageName)
                            } else {
                                onPlayPause()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (artBitmap != null) {
                    Image(
                        bitmap = artBitmap,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(colors.accent, colors.darkModuleBg)
                            )
                        )
                        // Sharp mecha equalizer bars placeholder
                        val barW = size.width * 0.11f
                        val heights = listOf(0.35f, 0.65f, 0.48f, 0.78f, 0.42f)
                        val gap = size.width * 0.055f
                        val startX = (size.width - (heights.size * barW + (heights.size - 1) * gap)) / 2f
                        heights.forEachIndexed { i, hFrac ->
                            val bh = size.height * hFrac
                            drawRect(
                                color = colors.onAccent.copy(alpha = 0.86f),
                                topLeft = Offset(startX + i * (barW + gap), size.height - bh - size.height * 0.12f),
                                size = androidx.compose.ui.geometry.Size(barW, bh)
                            )
                        }
                    }
                }

                if (media.isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(19.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.darkModuleBg.copy(alpha = 0.90f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .kansoTactileClickable(
                        onLongClick = onLongClick,
                        onClick = {
                            if (!notificationAccessGranted && !media.hasActiveSession && onRequestMediaPermission != null) {
                                onRequestMediaPermission()
                            } else {
                                onPlayPause()
                            }
                        }
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val sourceTag = when {
                    media.sourceAppLabel.isNotBlank() -> media.sourceAppLabel.uppercase()
                    !notificationAccessGranted -> "SYNC MEDIA"
                    else -> "DECK // HUD"
                }
                Text(
                    text = sourceTag,
                    color = colors.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.title.ifBlank { stringResource(R.string.media_hud_fallback_title) },
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.artist.ifBlank { stringResource(R.string.media_hud_fallback_subtitle) },
                    color = colors.inkSecondary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    lineHeight = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("media_hud_previous")
                            .size(width = 30.dp, height = 26.dp)
                            .clip(btnShape)
                            .background(colors.darkModuleBg)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onPrevious),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_media_previous),
                            tint = Color(0xFFF2F3F5),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("media_hud_play_pause_btn")
                            .size(width = 34.dp, height = 26.dp)
                            .clip(btnShape)
                            .background(if (media.isPlaying) colors.accent else colors.darkModuleElevated)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = media.isPlaying,
                            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.72f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.72f)) },
                            label = "mediaHudPlayIcon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.cd_media_play_pause),
                                tint = if (playing) colors.onAccent else Color(0xFFF2F3F5),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .testTag("media_hud_next")
                            .size(width = 30.dp, height = 26.dp)
                            .clip(btnShape)
                            .background(colors.darkModuleBg)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.cd_media_next),
                            tint = Color(0xFFF2F3F5),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
