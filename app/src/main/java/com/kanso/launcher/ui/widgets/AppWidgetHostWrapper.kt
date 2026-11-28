package com.kanso.launcher.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.kanso.launcher.data.db.HomeItemType
import com.kanso.launcher.data.model.AccentBlockStyle
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.service.MediaHudState
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class KansoWidgetTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String = "",
    val itemType: String,
    val spanCols: Int,
    val spanRows: Int,
    val config: ModularWidgetConfig = ModularWidgetConfig()
)

/**
 * Hosts a bound third-party Android AppWidget edge-to-edge (ZERO padding) and calls
 * updateAppWidgetSize dynamically whenever its measured cell bounds change.
 */
@Composable
fun ExternalAppWidgetCard(
    appWidgetHost: AppWidgetHost,
    appWidgetId: Int,
    isEditMode: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalKansoColors.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val info: AppWidgetProviderInfo? = remember(appWidgetId) { manager.getAppWidgetInfo(appWidgetId) }
    var hostViewRef by remember(appWidgetId) { mutableStateOf<AppWidgetHostView?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .testTag("external_widget_$appWidgetId")
            .fillMaxSize()
            .kansoIconNeumorphic(cornerRadius = 18.dp, shadowScale = 0.72f)
            .clip(RoundedCornerShape(18.dp))
    ) {
        val wDp = maxWidth.value.toInt().coerceAtLeast(48)
        val hDp = maxHeight.value.toInt().coerceAtLeast(48)

        LaunchedEffect(hostViewRef, wDp, hDp) {
            val view = hostViewRef ?: return@LaunchedEffect
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    view.updateAppWidgetSize(Bundle.EMPTY, listOf(SizeF(wDp.toFloat(), hDp.toFloat())))
                } else {
                    @Suppress("DEPRECATION")
                    view.updateAppWidgetSize(Bundle.EMPTY, wDp, hDp, wDp, hDp)
                }
            }
        }

        if (info != null) {
            AndroidView(
                factory = { ctx ->
                    appWidgetHost.createView(ctx.applicationContext, appWidgetId, info).apply {
                        setAppWidget(appWidgetId, info)
                        setPadding(0, 0, 0, 0)
                        hostViewRef = this
                    }
                },
                update = { view ->
                    view.setPadding(0, 0, 0, 0)
                    hostViewRef = view
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.darkModuleBg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Widgets,
                    contentDescription = null,
                    tint = colors.inkSecondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Widget unavailable",
                    color = colors.inkSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (isEditMode) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
                    .clip(CircleShape)
                    .kansoNeumorphic(raised = true, cornerRadius = 15.dp, surfaceColor = colors.accent)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove widget",
                    tint = colors.onAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Live-Preview Widget Add Dialog featuring:
 * - Pre-configured Mecha-Industrial Themes: Reactor Core, Tactical Gauge, and Data Stream
 * - Full modular Kanso widgets & third-party Android AppWidgets with dynamic real-size previews
 */
@Composable
fun ModularWidgetAddDialog(
    telemetry: TelemetryState,
    media: MediaHudState,
    weather: CachedWeather,
    audioBands: List<Float>,
    providers: List<AppWidgetProviderInfo>,
    allowExternalWidgets: Boolean = true,
    onAddKansoWidget: (KansoWidgetTemplate) -> Unit,
    onSelectExternalProvider: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    var activeTab by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableStateOf(StitchThemeFilter.KANSO_HYBRID) }
    val accentBlockTemplates = remember { buildAccentBlockTemplates() }

    val tabs = remember(allowExternalWidgets) {
        if (allowExternalWidgets) {
            listOf("Widget Matrix", "Accent Blocks", "Android")
        } else {
            listOf("Widget Matrix", "Accent Blocks")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .testTag("widget_add_preview_dialog")
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 28.dp)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Widget Matrix Picker",
                        color = colors.inkPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "LIVE MULTI-THEME CATALOG • ACCENT BLOCKS",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = colors.inkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    val selected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .testTag("widget_picker_tab_$idx")
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.accent else Color.Transparent)
                            .kansoTactileClickable { activeTab = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) colors.onAccent else colors.inkPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (activeTab) {
                0 -> {
                    StitchThemeFilterRibbon(
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WidgetMatrixCatalogContent(
                        telemetry = telemetry,
                        media = media,
                        weather = weather,
                        audioBands = audioBands,
                        use24HourClock = true,
                        selectedFilter = selectedFilter,
                        showPhysicsControls = false,
                        onAddWidget = { tmpl ->
                            onAddKansoWidget(tmpl)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    )
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(accentBlockTemplates, key = { it.id }) { tmpl ->
                            Column(
                                modifier = Modifier
                                    .testTag("widget_template_${tmpl.id}")
                                    .fillMaxWidth()
                                    .kansoNeumorphic(raised = false, cornerRadius = 20.dp)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tmpl.badge,
                                    color = colors.accent,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                )
                                AccentColorBlockWidget(
                                    style = tmpl.config.accentBlockStyle,
                                    flipped = tmpl.config.accentBlockFlipped,
                                    rotated = tmpl.config.accentBlockRotated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tmpl.title,
                                            color = colors.inkPrimary,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tmpl.subtitle,
                                            color = colors.inkSecondary,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.accent)
                                            .kansoTactileClickable {
                                                onAddKansoWidget(tmpl)
                                                onDismiss()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "PLACE",
                                            color = colors.onAccent,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    ExternalWidgetPickerList(
                        providers = providers,
                        onSelectProvider = {
                            onSelectExternalProvider(it)
                            onDismiss()
                        },
                        modifier = Modifier.heightIn(max = 430.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExternalWidgetPickerList(
    providers: List<AppWidgetProviderInfo>,
    onSelectProvider: (AppWidgetProviderInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalKansoColors.current
    val pm = context.packageManager
    val densityDpi = context.resources.displayMetrics.densityDpi

    if (providers.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No external Android widgets installed on this device.",
                color = colors.inkSecondary,
                fontSize = 13.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(providers, key = { it.provider.flattenToShortString() }) { info ->
            val label = remember(info) { info.loadLabel(pm) ?: info.provider.shortClassName }
            val pkgLabel = remember(info) {
                runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrDefault(info.provider.packageName)
            }
            val sizeTag = remember(info) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
                    "${info.targetCellWidth}×${info.targetCellHeight} cells (${info.minWidth}×${info.minHeight}dp)"
                } else {
                    "${info.minWidth}×${info.minHeight}dp"
                }
            }
            var previewBitmap by remember(info.provider) { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(info.provider) {
                previewBitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        val drawable = info.loadPreviewImage(context, densityDpi)
                            ?: info.loadIcon(context, densityDpi)
                        drawable?.toBitmap(
                            width = drawable.intrinsicWidth.coerceIn(96, 480),
                            height = drawable.intrinsicHeight.coerceIn(96, 320)
                        )?.asImageBitmap()
                    }.getOrNull()
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = false, cornerRadius = 18.dp)
                    .kansoTactileClickable { onSelectProvider(info) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.canvasBg.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = previewBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = label,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Widgets,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$pkgLabel • $sizeTag",
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add $label",
                            tint = colors.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
