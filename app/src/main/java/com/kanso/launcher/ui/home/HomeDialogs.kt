package com.kanso.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.data.db.HomeItemEntity
import com.kanso.launcher.data.db.HomeItemType
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.model.AccentBlockStyle
import com.kanso.launcher.data.model.ActionTileBinding
import com.kanso.launcher.data.model.KansoActionId
import com.kanso.launcher.data.model.MechaWidgetTheme
import com.kanso.launcher.data.model.ModularWidgetConfig
import com.kanso.launcher.data.model.TelemetryMetric
import com.kanso.launcher.data.prefs.BuiltInWidgetKey
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.data.prefs.KansoSettingsState
import com.kanso.launcher.ui.FolderWithApps
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.KansoAppIcon

/**
 * Unified Drawer-Style Icon App Picker (Grid / List toggle + live search + tap-to-select,
 * ZERO checkboxes, ZERO inner icon padding).
 */
@Composable
fun KansoAppPickerContent(
    apps: List<InstalledApp>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    selectedKeys: Set<String> = emptySet(),
    onAppClick: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    var search by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }

    val filtered = remember(apps, search) {
        val q = search.trim()
        if (q.isEmpty()) apps
        else apps.filter {
            it.label.contains(q, ignoreCase = true) ||
                it.packageName.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                },
                placeholder = { Text("Search apps", fontSize = 13.sp) },
                modifier = Modifier
                    .testTag("app_picker_search")
                    .weight(1f)
            )
            Box(
                modifier = Modifier
                    .testTag("app_picker_view_toggle")
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.darkModuleBg)
                    .kansoTactileClickable { isGridView = !isGridView },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                    contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                    tint = Color(0xFFF2F3F5),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps found",
                    color = colors.inkSecondary,
                    fontSize = 13.sp
                )
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .testTag("app_picker_grid")
                    .fillMaxWidth()
                    .heightIn(max = 270.dp)
            ) {
                items(filtered, key = { it.componentKey }) { app ->
                    val isSelected = app.componentKey in selectedKeys
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("app_picker_item_${app.packageName}")
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(colors.accent.copy(alpha = 0.14f))
                                        .border(1.5.dp, colors.accent, RoundedCornerShape(14.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .kansoTactileClickable { onAppClick(app) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            KansoAppIcon(
                                packageName = app.packageName,
                                activityName = app.activityName,
                                contentDescription = app.label,
                                iconPackManager = iconPackManager,
                                iconPackMode = iconPackMode,
                                selectedIconPackPackage = selectedIconPackPackage,
                                iconOverride = overrides[app.componentKey],
                                iconSize = 52.dp,
                                cornerRadius = 14.dp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = colors.onAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            color = if (isSelected) colors.accent else colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .testTag("app_picker_list")
                    .fillMaxWidth()
                    .heightIn(max = 270.dp)
            ) {
                items(filtered, key = { it.componentKey }) { app ->
                    val isSelected = app.componentKey in selectedKeys
                    Row(
                        modifier = Modifier
                            .testTag("app_picker_item_${app.packageName}")
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) colors.accent.copy(alpha = 0.14f) else colors.surfaceInset)
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, colors.accent, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .kansoTactileClickable { onAppClick(app) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KansoAppIcon(
                            packageName = app.packageName,
                            activityName = app.activityName,
                            contentDescription = app.label,
                            iconPackManager = iconPackManager,
                            iconPackMode = iconPackMode,
                            selectedIconPackPackage = selectedIconPackPackage,
                            iconOverride = overrides[app.componentKey],
                            iconSize = 42.dp,
                            cornerRadius = 11.dp
                        )
                        Text(
                            text = app.label,
                            color = if (isSelected) colors.accent else colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = colors.onAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularFolderDialog(
    folder: FolderWithApps,
    allApps: List<InstalledApp>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    badges: Map<String, Int>,
    onLaunchApp: (String, String) -> Unit,
    onRemoveApp: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    val appByKey = remember(allApps) { allApps.associateBy { it.componentKey } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 28.dp)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = folder.folder.title,
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close folder",
                        tint = colors.inkSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(folder.apps, key = { "${it.packageName}/${it.activityName}" }) { item ->
                    val key = "${item.packageName}/${item.activityName}"
                    val info = appByKey[key]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .kansoTactileClickable {
                                onLaunchApp(item.packageName, item.activityName)
                                onDismiss()
                            }
                            .padding(2.dp)
                    ) {
                        KansoAppIcon(
                            packageName = item.packageName,
                            activityName = item.activityName,
                            contentDescription = info?.label ?: item.packageName,
                            iconPackManager = iconPackManager,
                            iconPackMode = iconPackMode,
                            selectedIconPackPackage = selectedIconPackPackage,
                            iconOverride = overrides[key],
                            badgeCount = badges[item.packageName] ?: 0,
                            iconSize = 56.dp,
                            cornerRadius = 15.dp
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = info?.label ?: item.packageName.substringAfterLast('.'),
                            color = colors.inkPrimary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Remove",
                            color = colors.accent,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .clickable { onRemoveApp(item.packageName, item.activityName) }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Folder Creator using Drawer-Style Grid/List Icon App Picker (tap icons to select, ZERO checkboxes).
 */
@Composable
fun CreateFolderDialog(
    apps: List<InstalledApp>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    onCreate: (String, List<InstalledApp>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    var title by remember { mutableStateOf("Utilities") }
    val selected = remember { mutableStateListOf<InstalledApp>() }
    val selectedKeys = selected.map { it.componentKey }.toSet()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 28.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Circular Folder",
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.inkSecondary)
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("Folder name") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "TAP APPS TO SELECT (${selected.size} CHOSEN)",
                color = colors.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )

            KansoAppPickerContent(
                apps = apps,
                iconPackManager = iconPackManager,
                iconPackMode = iconPackMode,
                selectedIconPackPackage = selectedIconPackPackage,
                overrides = overrides,
                selectedKeys = selectedKeys,
                onAppClick = { app ->
                    if (selected.any { it.componentKey == app.componentKey }) {
                        selected.removeAll { it.componentKey == app.componentKey }
                    } else {
                        selected.add(app)
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .testTag("folder_create_cancel")
                        .clip(RoundedCornerShape(14.dp))
                        .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
                        .kansoTactileClickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text("Cancel", color = colors.inkSecondary, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .testTag("folder_create_confirm")
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected.isNotEmpty()) colors.accent else colors.trackMuted)
                        .kansoTactileClickable(enabled = selected.isNotEmpty()) {
                            onCreate(title, selected.toList())
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", color = colors.onAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Floating Dock Slot Picker using Drawer-Style Grid/List Icon App Picker (tap to assign immediately).
 */
@Composable
fun DockSlotPickerDialog(
    slotIndex: Int,
    apps: List<InstalledApp>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    onPickApp: (InstalledApp) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 26.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dock Slot ${slotIndex + 1} App",
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.inkSecondary)
                }
            }

            KansoAppPickerContent(
                apps = apps,
                iconPackManager = iconPackManager,
                iconPackMode = iconPackMode,
                selectedIconPackPackage = selectedIconPackPackage,
                overrides = overrides,
                onAppClick = { app ->
                    onPickApp(app)
                    onDismiss()
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .testTag("dock_slot_reset")
                        .clip(CircleShape)
                        .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                        .kansoTactileClickable {
                            onResetDefault()
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Reset Default", color = colors.accent, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .testTag("dock_slot_close")
                        .clip(CircleShape)
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable(onClick = onDismiss)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Close", color = Color(0xFFF2F3F5), fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Individual Widget Settings Dialog for a SINGLE Showcase Page 1 widget.
 * Long-pressing any Showcase widget opens ONLY that specific widget's settings (no combined menu).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleShowcaseWidgetDialog(
    widgetKey: String,
    settings: KansoSettingsState,
    onSetTheme: (MechaWidgetTheme) -> Unit,
    onToggleVisibility: () -> Unit,
    onSetTelemetryMetric: (TelemetryMetric) -> Unit,
    onSetTelemetryAction: (KansoActionId) -> Unit,
    onSetVolumeAction: (KansoActionId) -> Unit,
    onSetBatteryAction: (KansoActionId) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    var selectedTheme by remember(widgetKey) { mutableStateOf(settings.themeForShowcaseWidget(widgetKey)) }
    var metric by remember(widgetKey) { mutableStateOf(settings.page1TelemetryMetric) }
    var telAction by remember(widgetKey) { mutableStateOf(settings.page1TelemetryAction) }
    var volAction by remember(widgetKey) { mutableStateOf(settings.page1VolumeToggleAction) }
    var batAction by remember(widgetKey) { mutableStateOf(settings.page1BatteryAction) }
    val isVisible = widgetKey in settings.page1Widgets

    val title = when (widgetKey) {
        BuiltInWidgetKey.VOLUME_SWITCH -> "Sound / Toggle Switch"
        BuiltInWidgetKey.BATTERY -> "Battery Capsule"
        BuiltInWidgetKey.NETWORK -> "Telemetry Pill"
        BuiltInWidgetKey.QUICK_TOGGLES -> "Quick Toggle Cluster"
        BuiltInWidgetKey.MEDIA_HUD -> "Media HUD Widget"
        BuiltInWidgetKey.WEATHER -> "Weather Medallion"
        BuiltInWidgetKey.CALENDAR -> "Calendar Ribbon"
        else -> "Widget Settings"
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .testTag("single_showcase_widget_dialog")
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 26.dp)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = colors.inkPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "INDIVIDUAL WIDGET SETTINGS",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.inkSecondary)
                }
            }

            // Mecha-Industrial Theme Preset Picker for this individual widget
            Text(
                text = "MECHA-INDUSTRIAL THEME",
                color = colors.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MechaWidgetTheme.entries.forEach { theme ->
                    ChoiceChipPill(
                        label = theme.title,
                        selected = selectedTheme == theme,
                        onClick = { selectedTheme = theme }
                    )
                }
            }

            // Widget-specific controls ONLY for the pressed widget
            when (widgetKey) {
                BuiltInWidgetKey.VOLUME_SWITCH -> {
                    Text(
                        text = "SWITCH ACTION",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            KansoActionId.SILENT_MODE,
                            KansoActionId.TORCH,
                            KansoActionId.AUTO_ROTATE,
                            KansoActionId.POWER_SAVING,
                            KansoActionId.DND,
                            KansoActionId.DARK_MODE
                        ).forEach { act ->
                            ChoiceChipPill(
                                label = act.label,
                                selected = volAction == act,
                                onClick = { volAction = act }
                            )
                        }
                    }
                }

                BuiltInWidgetKey.BATTERY -> {
                    Text(
                        text = "BATTERY TAP ACTION",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            KansoActionId.BATTERY_SETTINGS,
                            KansoActionId.POWER_SAVING,
                            KansoActionId.OPEN_SETTINGS
                        ).forEach { act ->
                            ChoiceChipPill(
                                label = act.label,
                                selected = batAction == act,
                                onClick = { batAction = act }
                            )
                        }
                    }
                }

                BuiltInWidgetKey.NETWORK -> {
                    Text(
                        text = "TELEMETRY METRIC",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TelemetryMetric.entries.forEach { m ->
                            ChoiceChipPill(
                                label = m.title,
                                selected = metric == m,
                                onClick = { metric = m }
                            )
                        }
                    }

                    Text(
                        text = "TAP ACTION",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            KansoActionId.WIFI,
                            KansoActionId.MOBILE_DATA,
                            KansoActionId.BATTERY_SETTINGS,
                            KansoActionId.SYSTEM_SETTINGS,
                            KansoActionId.OPEN_SETTINGS
                        ).forEach { act ->
                            ChoiceChipPill(
                                label = act.label,
                                selected = telAction == act,
                                onClick = { telAction = act }
                            )
                        }
                    }
                }
            }

            if (widgetKey != BuiltInWidgetKey.VOLUME_SWITCH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show on Home Showcase",
                        color = colors.inkPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { onToggleVisibility() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onAccent,
                            checkedTrackColor = colors.accent
                        )
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .testTag("single_widget_apply_btn")
                        .clip(CircleShape)
                        .background(colors.accent)
                        .kansoTactileClickable {
                            onSetTheme(selectedTheme)
                            when (widgetKey) {
                                BuiltInWidgetKey.VOLUME_SWITCH -> onSetVolumeAction(volAction)
                                BuiltInWidgetKey.BATTERY -> onSetBatteryAction(batAction)
                                BuiltInWidgetKey.NETWORK -> {
                                    onSetTelemetryMetric(metric)
                                    onSetTelemetryAction(telAction)
                                }
                            }
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "APPLY",
                        color = colors.onAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

/**
 * Individual Widget Settings Dialog for any Workspace or Lockscreen widget:
 * - Mecha-Industrial Theme preset (Default / Reactor Core / Tactical Gauge / Data Stream)
 * - Dynamic Adaptive Grid Size (1..maxCols × 1..5)
 * - Metric / Toggle / Action Matrix per-tile system action or app remapping
 * - Direct Remove button for that widget
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModularWidgetConfigDialog(
    item: HomeItemEntity,
    maxCols: Int,
    apps: List<InstalledApp> = emptyList(),
    iconPackManager: IconPackManager? = null,
    iconPackMode: IconPackMode = IconPackMode.KANSO_ACCENT,
    selectedIconPackPackage: String = "",
    overrides: Map<String, IconOverrideEntity> = emptyMap(),
    onSaveConfig: (ModularWidgetConfig, Int, Int, String) -> Unit,
    onRemoveWidget: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    val initial = remember(item.id, item.configJson) { ModularWidgetConfig.decode(item.configJson) }

    var mechaTheme by remember(item.id) { mutableStateOf(initial.mechaTheme) }
    var metric by remember(item.id) { mutableStateOf(initial.telemetryMetric) }
    var tapAction by remember(item.id) { mutableStateOf(initial.tapAction) }
    var toggleAction by remember(item.id) { mutableStateOf(initial.toggleAction) }
    var matrixRows by remember(item.id) { mutableIntStateOf(initial.matrixRows.coerceIn(1, 4)) }
    var matrixCols by remember(item.id) { mutableIntStateOf(initial.matrixCols.coerceIn(1, 4)) }
    val matrixTiles = remember(item.id) {
        mutableStateListOf<ActionTileBinding>().apply {
            addAll(initial.matrixTiles)
        }
    }
    var editingMatrixTileIndex by remember { mutableStateOf<Int?>(null) }
    var showVisualizer by remember(item.id) { mutableStateOf(initial.showAudioVisualizer) }
    var accentBlockStyle by remember(item.id) { mutableStateOf(initial.accentBlockStyle) }
    var accentBlockFlipped by remember(item.id) { mutableStateOf(initial.accentBlockFlipped) }
    var accentBlockRotated by remember(item.id) { mutableStateOf(initial.accentBlockRotated) }
    var spanCols by remember(item.id) { mutableIntStateOf(item.spanColumns.coerceIn(1, maxCols)) }
    var spanRows by remember(item.id) { mutableIntStateOf(item.spanRows.coerceIn(1, 5)) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .testTag("modular_widget_config_dialog")
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 26.dp)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.label.ifBlank { "Widget Settings" },
                        color = colors.inkPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "INDIVIDUAL WIDGET CONFIG",
                        color = colors.accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.inkSecondary)
                }
            }

            // Dynamic Adaptive Grid Span Resizer
            Text(
                text = "DYNAMIC GRID SIZE (${spanCols}W × ${spanRows}H)",
                color = colors.accent,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepperPill(
                    label = "WIDTH: $spanCols",
                    onMinus = { if (spanCols > 1) spanCols-- },
                    onPlus = { if (spanCols < maxCols) spanCols++ },
                    modifier = Modifier.weight(1f)
                )
                StepperPill(
                    label = "HEIGHT: $spanRows",
                    onMinus = { if (spanRows > 1) spanRows-- },
                    onPlus = { if (spanRows < 5) spanRows++ },
                    modifier = Modifier.weight(1f)
                )
            }

            // Mecha-Industrial Theme Preset Selector (for all Kanso modular widgets except pure Accent Blocks)
            if (item.itemType != HomeItemType.EXTERNAL_WIDGET && item.itemType != HomeItemType.ACCENT_BLOCK) {
                Text(
                    text = "MECHA-INDUSTRIAL THEME",
                    color = colors.inkSecondary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MechaWidgetTheme.entries.forEach { t ->
                        ChoiceChipPill(
                            label = t.title,
                            selected = mechaTheme == t,
                            onClick = { mechaTheme = t }
                        )
                    }
                }
            }

            when (item.itemType) {
                HomeItemType.ACCENT_BLOCK -> {
                    Text(
                        text = "ACCENT BLOCK ARCHITECTURE",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AccentBlockStyle.entries.forEach { style ->
                            ChoiceChipPill(
                                label = style.title,
                                selected = accentBlockStyle == style,
                                onClick = { accentBlockStyle = style }
                            )
                        }
                    }
                    Text(
                        text = "ORIENTATION",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChoiceChipPill(
                            label = if (accentBlockFlipped) "Flip Horizontal: ON" else "Flip Horizontal: OFF",
                            selected = accentBlockFlipped,
                            onClick = { accentBlockFlipped = !accentBlockFlipped }
                        )
                        ChoiceChipPill(
                            label = if (accentBlockRotated) "Rotate 180°: ON" else "Rotate 180°: OFF",
                            selected = accentBlockRotated,
                            onClick = { accentBlockRotated = !accentBlockRotated }
                        )
                    }
                }

                HomeItemType.TELEMETRY_CAPSULE -> {
                    Text(
                        text = "TELEMETRY METRIC",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryMetric.entries.forEach { m ->
                            ChoiceChipPill(
                                label = m.title,
                                selected = metric == m,
                                onClick = { metric = m }
                            )
                        }
                    }

                    ChoiceChipPill(
                        label = if (showVisualizer) "Audio Dot Visualizer: ON" else "Audio Dot Visualizer: OFF",
                        selected = showVisualizer,
                        onClick = { showVisualizer = !showVisualizer }
                    )

                    Text(
                        text = "TAP ACTION",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KansoActionId.entries.filter { it != KansoActionId.LAUNCH_APP }.forEach { act ->
                            ChoiceChipPill(
                                label = act.shortLabel,
                                selected = tapAction == act,
                                onClick = { tapAction = act }
                            )
                        }
                    }
                }

                HomeItemType.ACTION_MATRIX -> {
                    Text(
                        text = "MATRIX DIMENSIONS (${matrixCols} COLS × ${matrixRows} ROWS)",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StepperPill(
                            label = "COLS: $matrixCols",
                            onMinus = { if (matrixCols > 1) matrixCols-- },
                            onPlus = { if (matrixCols < 4) matrixCols++ },
                            modifier = Modifier.weight(1f)
                        )
                        StepperPill(
                            label = "ROWS: $matrixRows",
                            onMinus = { if (matrixRows > 1) matrixRows-- },
                            onPlus = { if (matrixRows < 4) matrixRows++ },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val needed = (matrixRows * matrixCols).coerceIn(1, 16)
                    while (matrixTiles.size < needed) {
                        val defaults = ModularWidgetConfig.defaultMatrixTiles(needed)
                        matrixTiles.add(defaults[matrixTiles.size])
                    }

                    Text(
                        text = "TAP ANY SLOT TO REMAP ACTION / APP",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (idx in 0 until needed) {
                            val binding = matrixTiles[idx]
                            val label = binding.customLabel.ifBlank { binding.action.shortLabel }
                            ChoiceChipPill(
                                label = "${idx + 1}. $label",
                                selected = editingMatrixTileIndex == idx,
                                onClick = { editingMatrixTileIndex = idx }
                            )
                        }
                    }
                }

                HomeItemType.TOGGLE_SWITCH -> {
                    Text(
                        text = "TOGGLE SWITCH TARGET",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            KansoActionId.SILENT_MODE,
                            KansoActionId.TORCH,
                            KansoActionId.AUTO_ROTATE,
                            KansoActionId.POWER_SAVING,
                            KansoActionId.DND,
                            KansoActionId.WIFI,
                            KansoActionId.MOBILE_DATA,
                            KansoActionId.BLUETOOTH,
                            KansoActionId.HOTSPOT,
                            KansoActionId.AIRPLANE,
                            KansoActionId.DARK_MODE,
                            KansoActionId.MEDIA_PLAY_PAUSE,
                            KansoActionId.LOCK_SCREEN,
                            KansoActionId.RECENTS,
                            KansoActionId.SCREENSHOT
                        ).forEach { act ->
                            ChoiceChipPill(
                                label = act.label,
                                selected = toggleAction == act,
                                onClick = { toggleAction = act }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRemoveWidget != null) {
                    Row(
                        modifier = Modifier
                            .testTag("widget_config_remove_btn")
                            .clip(CircleShape)
                            .background(colors.surfaceInset)
                            .kansoTactileClickable {
                                onRemoveWidget()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove widget",
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "REMOVE",
                            color = colors.accent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Box(
                    modifier = Modifier
                        .testTag("widget_config_apply_btn")
                        .clip(CircleShape)
                        .background(colors.accent)
                        .kansoTactileClickable {
                            val updatedTiles = if (item.itemType == HomeItemType.ACTION_MATRIX) {
                                val needed = (matrixRows * matrixCols).coerceIn(1, 16)
                                matrixTiles.take(needed)
                            } else {
                                initial.matrixTiles
                            }
                            val nextConfig = initial.copy(
                                mechaTheme = mechaTheme,
                                telemetryMetric = metric,
                                tapAction = tapAction,
                                toggleAction = toggleAction,
                                matrixRows = matrixRows,
                                matrixCols = matrixCols,
                                matrixTiles = updatedTiles,
                                showAudioVisualizer = showVisualizer,
                                accentBlockStyle = accentBlockStyle,
                                accentBlockFlipped = accentBlockFlipped,
                                accentBlockRotated = accentBlockRotated,
                                customLabel = if (item.itemType == HomeItemType.TOGGLE_SWITCH) toggleAction.shortLabel else initial.customLabel
                            )
                            val nextLabel = when (item.itemType) {
                                HomeItemType.ACCENT_BLOCK -> accentBlockStyle.title
                                HomeItemType.TELEMETRY_CAPSULE -> metric.title
                                HomeItemType.TOGGLE_SWITCH -> toggleAction.label
                                HomeItemType.ACTION_MATRIX -> "${matrixCols}×${matrixRows} Matrix"
                                else -> item.label
                            }
                            onSaveConfig(nextConfig, spanCols, spanRows, nextLabel)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "APPLY",
                        color = colors.onAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }

    // Per-slot remap modal for Action Matrix tiles
    editingMatrixTileIndex?.let { slotIdx ->
        ActionTileBindingDialog(
            title = "Matrix Slot ${slotIdx + 1}",
            current = matrixTiles.getOrNull(slotIdx) ?: ActionTileBinding(KansoActionId.WIFI),
            apps = apps,
            iconPackManager = iconPackManager,
            iconPackMode = iconPackMode,
            selectedIconPackPackage = selectedIconPackPackage,
            overrides = overrides,
            onSelect = { newBinding ->
                if (slotIdx in matrixTiles.indices) {
                    matrixTiles[slotIdx] = newBinding
                }
                editingMatrixTileIndex = null
            },
            onDismiss = { editingMatrixTileIndex = null }
        )
    }
}

/**
 * Reusable dialog to map a button (Control Panel or Action Matrix) to ANY system action
 * OR any installed app via the Drawer-Style Icon App Picker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionTileBindingDialog(
    title: String,
    current: ActionTileBinding?,
    apps: List<InstalledApp>,
    iconPackManager: IconPackManager?,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    onSelect: (ActionTileBinding) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    var showAppsTab by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .testTag("action_tile_binding_dialog")
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 26.dp)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.inkSecondary)
                }
            }

            if (iconPackManager != null && apps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .kansoNeumorphic(raised = false, cornerRadius = 18.dp)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("binding_tab_actions")
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!showAppsTab) colors.accent else Color.Transparent)
                            .kansoTactileClickable { showAppsTab = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SYSTEM ACTIONS",
                            color = if (!showAppsTab) colors.onAccent else colors.inkPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .testTag("binding_tab_apps")
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (showAppsTab) colors.accent else Color.Transparent)
                            .kansoTactileClickable { showAppsTab = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Apps,
                                contentDescription = null,
                                tint = if (showAppsTab) colors.onAccent else colors.inkPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "APP SHORTCUT",
                                color = if (showAppsTab) colors.onAccent else colors.inkPrimary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            if (showAppsTab && iconPackManager != null) {
                KansoAppPickerContent(
                    apps = apps,
                    iconPackManager = iconPackManager,
                    iconPackMode = iconPackMode,
                    selectedIconPackPackage = selectedIconPackPackage,
                    overrides = overrides,
                    onAppClick = { app ->
                        onSelect(
                            ActionTileBinding(
                                action = KansoActionId.LAUNCH_APP,
                                packageName = app.packageName,
                                activityName = app.activityName,
                                customLabel = app.label
                            )
                        )
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        KansoActionId.entries.filter { it != KansoActionId.LAUNCH_APP }.forEach { act ->
                            ChoiceChipPill(
                                label = act.label,
                                selected = current?.action == act,
                                onClick = {
                                    onSelect(ActionTileBinding(action = act))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperPill(
    label: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = modifier
            .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "−",
            color = colors.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onMinus)
                .padding(horizontal = 6.dp)
        )
        Text(
            text = label,
            color = colors.inkPrimary,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
        Text(
            text = "+",
            color = colors.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onPlus)
                .padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun ChoiceChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) colors.accent else colors.surfaceInset)
            .kansoTactileClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) colors.onAccent else colors.inkPrimary,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp
        )
    }
}
