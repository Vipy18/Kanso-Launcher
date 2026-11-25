package com.kanso.launcher.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoNeumorphic

/**
 * Full-bleed edge-to-edge app icon renderer with subtle outer neumorphic elevation shadow
 * using the active IconPackManager.
 */
@Composable
fun KansoAppIcon(
    packageName: String,
    activityName: String,
    contentDescription: String?,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    iconOverride: IconOverrideEntity?,
    badgeCount: Int = 0,
    iconSize: Dp = 56.dp,
    cornerRadius: Dp? = null,
    tintOverride: Color? = null,
    showNeumorphicShadow: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val effectiveInk = tintOverride ?: colors.inkPrimary
    var bitmap by remember(
        packageName,
        activityName,
        iconPackMode,
        selectedIconPackPackage,
        iconOverride,
        effectiveInk,
        colors.accent
    ) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(
        packageName,
        activityName,
        iconPackMode,
        selectedIconPackPackage,
        iconOverride,
        effectiveInk,
        colors.accent
    ) {
        bitmap = iconPackManager.loadAppIcon(
            packageName = packageName,
            activityName = activityName,
            mode = iconPackMode,
            selectedPackPackage = selectedIconPackPackage,
            accentColor = colors.accent,
            inkColor = effectiveInk,
            overridePackPackage = iconOverride?.iconPackPackage,
            overrideDrawableName = iconOverride?.drawableName
        )
    }

    val clipCorner = cornerRadius ?: (iconSize * 0.26f).coerceAtLeast(8.dp)

    Box(
        modifier = modifier
            .size(iconSize)
            .then(
                if (showNeumorphicShadow) {
                    Modifier.kansoIconNeumorphic(cornerRadius = clipCorner, shadowScale = 0.56f)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(clipCorner))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(clipCorner))
                    .background(colors.darkModuleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = contentDescription,
                    tint = effectiveInk.copy(alpha = 0.82f),
                    modifier = Modifier.size(iconSize * 0.56f)
                )
            }
        }

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
        }
    }
}

/**
 * Rounded-square 2x2 mini-grid app folder icon with tactile neumorphic raised shadows.
 */
@Composable
fun CircularFolderIcon(
    apps: List<Pair<String, String>>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    containerSize: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    val topFour = apps.take(4)
    val folderRadius = (containerSize * 0.28f).coerceIn(14.dp, 20.dp)
    val innerPad = 5.dp
    val miniIconSize = ((containerSize - innerPad * 2 - 4.dp) / 2f).coerceAtLeast(16.dp)

    Box(
        modifier = modifier
            .size(containerSize)
            .kansoNeumorphic(
                raised = true,
                cornerRadius = folderRadius,
                surfaceColor = colors.darkModuleBg
            )
            .clip(RoundedCornerShape(folderRadius))
            .padding(innerPad),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            for (row in 0 until 2) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    for (col in 0 until 2) {
                        val idx = row * 2 + col
                        val entry = topFour.getOrNull(idx)
                        if (entry != null) {
                            val key = "${entry.first}/${entry.second}"
                            KansoAppIcon(
                                packageName = entry.first,
                                activityName = entry.second,
                                contentDescription = null,
                                iconPackManager = iconPackManager,
                                iconPackMode = iconPackMode,
                                selectedIconPackPackage = selectedIconPackPackage,
                                iconOverride = overrides[key],
                                iconSize = miniIconSize,
                                cornerRadius = (miniIconSize * 0.26f).coerceAtLeast(5.dp),
                                tintOverride = Color(0xFFF2F3F5),
                                showNeumorphicShadow = false
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(miniIconSize * 0.88f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2B2D34))
                            )
                        }
                    }
                }
            }
        }
    }
}
