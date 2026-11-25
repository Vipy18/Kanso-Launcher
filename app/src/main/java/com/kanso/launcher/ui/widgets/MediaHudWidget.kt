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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
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
import com.kanso.launcher.ui.theme.GeistFontFamily
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.WithMechaWidgetTheme
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.theme.mechaMetallicChassis

/**
 * Classic tactile Media HUD card with corner registration brackets, rounded-square album art,
 * and circular playback buttons (DEFAULT), or optional machined Mecha Media Deck.
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
        val isClassic = mechaTheme == MechaWidgetTheme.DEFAULT
        val artRadius = if (isClassic) 16.dp else 7.dp
        val artShape = RoundedCornerShape(artRadius)
        val btnShape = if (isClassic) CircleShape else RoundedCornerShape(5.dp)

        Row(
            modifier = modifier
                .testTag("widget_media_hud")
                .height(if (isClassic) 96.dp else 88.dp)
                .then(
                    if (isClassic) {
                        Modifier
                            .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
                            .kansoMechaAccents(
                                accentActive = media.isPlaying,
                                showCornerBrackets = true,
                                showLedPip = false
                            )
                    } else {
                        Modifier.mechaMetallicChassis(
                            theme = mechaTheme,
                            darkPlate = false,
                            accentActive = media.isPlaying,
                            cutDp = 12.dp
                        )
                    }
                )
                .padding(if (isClassic) 10.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .testTag("media_hud_art_play_pause")
                    .size(if (isClassic) 74.dp else 72.dp)
                    .kansoIconNeumorphic(cornerRadius = artRadius, shadowScale = 0.55f)
                    .clip(artShape)
                    .background(
                        if (isClassic) Brush.linearGradient(listOf(colors.accent, colors.accent.copy(alpha = 0.85f)))
                        else Brush.linearGradient(listOf(colors.accent, colors.darkModuleBg))
                    )
                    .then(
                        if (!isClassic) Modifier.border(1.dp, colors.accent.copy(alpha = 0.55f), artShape)
                        else Modifier
                    )
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
                } else if (isClassic) {
                    // Classic warm editorial silhouette bust on Signal Amber album frame
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val bustColor = Color(0xFF17181B)
                        drawCircle(
                            color = bustColor,
                            radius = w * 0.21f,
                            center = Offset(w * 0.50f, h * 0.36f)
                        )
                        val shoulders = Path().apply {
                            moveTo(w * 0.14f, h)
                            cubicTo(
                                w * 0.16f, h * 0.62f,
                                w * 0.30f, h * 0.57f,
                                w * 0.50f, h * 0.57f
                            )
                            cubicTo(
                                w * 0.70f, h * 0.57f,
                                w * 0.84f, h * 0.62f,
                                w * 0.86f, h
                            )
                            close()
                        }
                        drawPath(path = shoulders, color = bustColor)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(colors.accent, colors.darkModuleBg)
                            )
                        )
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
                            .padding(4.dp)
                            .size(19.dp)
                            .clip(if (isClassic) CircleShape else RoundedCornerShape(6.dp))
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

            Spacer(modifier = Modifier.width(10.dp))

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
                    isClassic -> "MEDIA HUD"
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
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = media.artist.ifBlank { stringResource(R.string.media_hud_fallback_subtitle) },
                    color = colors.inkSecondary,
                    fontFamily = GeistFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.5.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val transportSize = if (isClassic) 26.dp else 28.dp
                    Box(
                        modifier = Modifier
                            .testTag("media_hud_previous")
                            .size(width = if (isClassic) transportSize else 30.dp, height = transportSize)
                            .clip(btnShape)
                            .background(if (isClassic) colors.surfaceInset else colors.darkModuleBg)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onPrevious),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_media_previous),
                            tint = colors.inkPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .testTag("media_hud_play_pause_btn")
                            .size(width = if (isClassic) transportSize else 34.dp, height = transportSize)
                            .clip(btnShape)
                            .background(
                                when {
                                    media.isPlaying -> colors.accent
                                    isClassic -> colors.surfaceInset
                                    else -> colors.darkModuleElevated
                                }
                            )
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
                                tint = if (playing) colors.onAccent else colors.inkPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .testTag("media_hud_next")
                            .size(width = if (isClassic) transportSize else 30.dp, height = transportSize)
                            .clip(btnShape)
                            .background(if (isClassic) colors.surfaceInset else colors.darkModuleBg)
                            .kansoTactileClickable(onLongClick = onLongClick, onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = stringResource(R.string.cd_media_next),
                            tint = colors.inkPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
