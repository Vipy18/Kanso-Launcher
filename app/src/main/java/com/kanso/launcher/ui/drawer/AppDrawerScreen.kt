package com.kanso.launcher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kanso.launcher.R
import com.kanso.launcher.data.apps.AppDrawerCategory
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.data.db.SurfacePage
import com.kanso.launcher.data.iconpacks.IconPackInfo
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.prefs.DrawerViewMode
import com.kanso.launcher.ui.LauncherViewModel
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.KansoAppIcon
import kotlinx.coroutines.launch

private val ALPHABET = (('A'..'Z').toList() + '#')

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit
) {
    BackHandler { onClose() }

    val colors = LocalKansoColors.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val recentPackages by viewModel.recentAppPackages.collectAsStateWithLifecycle()
    val overrides by viewModel.iconOverrides.collectAsStateWithLifecycle()
    val badges by viewModel.notificationBadges.collectAsStateWithLifecycle()
    val installedPacks by viewModel.installedIconPacks.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppDrawerCategory.ALL) }
    var activeScrubLetter by remember { mutableStateOf<Char?>(null) }
    var contextMenuApp by remember { mutableStateOf<InstalledApp?>(null) }
    var iconPickerApp by remember { mutableStateOf<InstalledApp?>(null) }
    var pinToDockApp by remember { mutableStateOf<InstalledApp?>(null) }
    var headerDismissDrag by remember { mutableFloatStateOf(0f) }

    val categoryCounts = remember(visibleApps) {
        AppDrawerCategory.entries.associateWith { cat ->
            if (cat == AppDrawerCategory.ALL) visibleApps.size
            else visibleApps.count { it.category == cat }
        }
    }

    val recentApps = remember(visibleApps, recentPackages) {
        val byPkg = visibleApps.associateBy { it.packageName }
        val resolved = recentPackages.mapNotNull { byPkg[it] }
        if (resolved.isNotEmpty()) resolved.take(6) else visibleApps.take(6)
    }

    val filteredApps by remember(visibleApps, searchQuery, selectedCategory) {
        derivedStateOf {
            val byCategory = if (selectedCategory == AppDrawerCategory.ALL) {
                visibleApps
            } else {
                visibleApps.filter { it.category == selectedCategory }
            }
            val q = searchQuery.trim()
            if (q.isEmpty()) byCategory
            else byCategory.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true) ||
                    it.subsystemTag.contains(q, ignoreCase = true)
            }
        }
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val firstVisibleLetter by remember(filteredApps, settings.drawerViewMode) {
        derivedStateOf {
            val idx = if (settings.drawerViewMode == DrawerViewMode.GRID) {
                gridState.firstVisibleItemIndex
            } else {
                listState.firstVisibleItemIndex
            }
            filteredApps.getOrNull(idx)?.sectionLetter ?: 'A'
        }
    }

    val gridCols = settings.homeGridColumns.coerceIn(4, 5)
    val tileIconSize = if (gridCols >= 5) 36.dp else 42.dp

    Box(
        modifier = Modifier
            .testTag("stitch_app_drawer")
            .fillMaxSize()
            .background(colors.canvasBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top HUD Status Line (Swipe down here dismisses Drawer)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { headerDismissDrag = 0f },
                            onVerticalDrag = { _, dragAmount -> headerDismissDrag += dragAmount },
                            onDragEnd = {
                                if (headerDismissDrag > 90f) onClose()
                                headerDismissDrag = 0f
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                            Text(
                                text = "SYS_DRAWER // INDEX V4.2",
                                color = colors.accent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "PACKAGES: ${visibleApps.size} / SYNCED • RAM ${telemetry.freeRamText} FREE",
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .testTag("drawer_layout_toggle")
                                .size(40.dp)
                                .kansoNeumorphic(raised = true, cornerRadius = 13.dp)
                                .kansoTactileClickable { viewModel.toggleDrawerViewMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.drawerViewMode == DrawerViewMode.GRID) {
                                    Icons.AutoMirrored.Rounded.ViewList
                                } else {
                                    Icons.Rounded.GridView
                                },
                                contentDescription = stringResource(R.string.cd_drawer_layout_toggle),
                                tint = colors.inkPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .testTag("drawer_settings_button")
                                .size(40.dp)
                                .kansoNeumorphic(raised = true, cornerRadius = 13.dp)
                                .kansoTactileClickable(onClick = onOpenSettings),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.cd_open_settings),
                                tint = colors.accent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Recessed Tactical Search Dock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .kansoNeumorphic(raised = false, cornerRadius = 16.dp, surfaceColor = colors.surfaceInset)
                        .border(
                            width = 1.dp,
                            color = if (searchQuery.isNotEmpty()) colors.accent.copy(alpha = 0.65f) else colors.trackMuted.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "FIND APPLICATION OR PACKAGE...",
                                color = colors.inkSecondary.copy(alpha = 0.72f),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.5.sp,
                                letterSpacing = 0.6.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.inkPrimary,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier
                                .testTag("drawer_search_input")
                                .fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear_search),
                            tint = colors.inkSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    AppDrawerCategory.entries.forEach { cat ->
                        val count = categoryCounts[cat] ?: 0
                        val selected = selectedCategory == cat
                        Row(
                            modifier = Modifier
                                .testTag("drawer_cat_${cat.name.lowercase()}")
                                .clip(CircleShape)
                                .background(if (selected) colors.accent else colors.surfaceRaised)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) colors.accent else colors.trackMuted.copy(alpha = 0.45f),
                                    shape = CircleShape
                                )
                                .kansoTactileClickable { selectedCategory = cat }
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cat.label,
                                color = if (selected) colors.onAccent else colors.inkPrimary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$count",
                                color = if (selected) colors.onAccent.copy(alpha = 0.78f) else colors.inkSecondary,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.5.sp
                            )
                        }
                    }
                }
            }

            // RECENTLY OPENED // LRU_CACHE Carousel (shown when not searching)
            if (searchQuery.isBlank() && recentApps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "RECENTLY OPENED // LRU_CACHE",
                        color = colors.inkSecondary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.9.sp,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentApps, key = { "lru_${it.componentKey}" }) { app ->
                            val badge = badges[app.packageName] ?: 0
                            Row(
                                modifier = Modifier
                                    .testTag("drawer_recent_${app.packageName}")
                                    .kansoNeumorphic(raised = true, cornerRadius = 16.dp)
                                    .combinedClickable(
                                        onClick = { viewModel.appRepository.launchApp(app.packageName, app.activityName) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            contextMenuApp = app
                                        }
                                    )
                                    .padding(start = 7.dp, end = 11.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .kansoNeumorphic(raised = false, cornerRadius = 10.dp, surfaceColor = colors.surfaceInset),
                                    contentAlignment = Alignment.Center
                                ) {
                                    KansoAppIcon(
                                        packageName = app.packageName,
                                        activityName = app.activityName,
                                        contentDescription = app.label,
                                        iconPackManager = viewModel.iconPackManager,
                                        iconPackMode = settings.iconPackMode,
                                        selectedIconPackPackage = settings.selectedIconPackPackage,
                                        iconOverride = overrides[app.componentKey],
                                        iconSize = 24.dp,
                                        cornerRadius = 7.dp
                                    )
                                }
                                Text(
                                    text = app.label,
                                    color = colors.inkPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (badge > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(colors.accent)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Drawer Matrix + Right-Edge Recessed A-Z Scrubber Rail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "NO APPS IN ${selectedCategory.label}" else "NO APPS MATCHING \"$searchQuery\"",
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp
                        )
                    }
                } else if (settings.drawerViewMode == DrawerViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridCols),
                        state = gridState,
                        contentPadding = PaddingValues(start = 14.dp, end = 36.dp, top = 4.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .testTag("drawer_apps_grid")
                            .fillMaxSize()
                    ) {
                        items(filteredApps, key = { it.componentKey }) { app ->
                            val badge = badges[app.packageName] ?: 0
                            // Stitch Raised Tactile Rounded-Square Tile with Recessed Dark Icon Well
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .testTag("drawer_app_${app.packageName}")
                                    .kansoNeumorphic(raised = true, cornerRadius = 20.dp)
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.appRepository.launchApp(app.packageName, app.activityName)
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            contextMenuApp = app
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 9.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Recessed Dark Tactile Well framing the real Android App Icon
                                    Box(
                                        modifier = Modifier
                                            .size(tileIconSize + 12.dp)
                                            .kansoNeumorphic(
                                                raised = false,
                                                cornerRadius = 14.dp,
                                                surfaceColor = colors.surfaceInset
                                            )
                                            .border(
                                                width = 0.8.dp,
                                                color = if (badge > 0) colors.accent.copy(alpha = 0.5f) else colors.trackMuted.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        KansoAppIcon(
                                            packageName = app.packageName,
                                            activityName = app.activityName,
                                            contentDescription = app.label,
                                            iconPackManager = viewModel.iconPackManager,
                                            iconPackMode = settings.iconPackMode,
                                            selectedIconPackPackage = settings.selectedIconPackPackage,
                                            iconOverride = overrides[app.componentKey],
                                            badgeCount = 0,
                                            iconSize = tileIconSize,
                                            cornerRadius = 10.dp
                                        )
                                    }

                                    if (badge > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .clip(CircleShape)
                                                .background(colors.accent)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (badge > 99) "99+" else "$badge",
                                                color = colors.onAccent,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 7.5.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = app.label,
                                    color = colors.inkPrimary,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = app.subsystemTag,
                                    color = colors.inkSecondary.copy(alpha = 0.72f),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 7.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 38.dp, top = 4.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier
                            .testTag("drawer_apps_list")
                            .fillMaxSize()
                    ) {
                        items(filteredApps, key = { it.componentKey }) { app ->
                            val badge = badges[app.packageName] ?: 0
                            Row(
                                modifier = Modifier
                                    .testTag("drawer_app_${app.packageName}")
                                    .fillMaxWidth()
                                    .kansoNeumorphic(raised = true, cornerRadius = 18.dp)
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.appRepository.launchApp(app.packageName, app.activityName)
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            contextMenuApp = app
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .kansoNeumorphic(raised = false, cornerRadius = 13.dp, surfaceColor = colors.surfaceInset),
                                    contentAlignment = Alignment.Center
                                ) {
                                    KansoAppIcon(
                                        packageName = app.packageName,
                                        activityName = app.activityName,
                                        contentDescription = app.label,
                                        iconPackManager = viewModel.iconPackManager,
                                        iconPackMode = settings.iconPackMode,
                                        selectedIconPackPackage = settings.selectedIconPackPackage,
                                        iconOverride = overrides[app.componentKey],
                                        badgeCount = badge,
                                        iconSize = 36.dp,
                                        cornerRadius = 9.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = colors.inkPrimary,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${app.subsystemTag} // ${app.packageName}",
                                        color = colors.inkSecondary,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Right-edge Recessed Vertical A-Z Scrubber Rail
                AlphabetScrubberRail(
                    highlightedLetter = activeScrubLetter ?: firstVisibleLetter,
                    onLetterSelected = { letter ->
                        if (activeScrubLetter != letter) {
                            activeScrubLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val targetIdx = filteredApps.indexOfFirst { it.sectionLetter >= letter }
                                .takeIf { it >= 0 } ?: (filteredApps.lastIndex.coerceAtLeast(0))
                            scope.launch {
                                if (settings.drawerViewMode == DrawerViewMode.GRID) {
                                    gridState.scrollToItem(targetIdx)
                                } else {
                                    listState.scrollToItem(targetIdx)
                                }
                            }
                        }
                    },
                    onScrubEnd = { activeScrubLetter = null },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 4.dp, bottom = 10.dp)
                )

                // Glowing Signal Amber Letter Bubble while scrubbing
                activeScrubLetter?.let { letter ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 44.dp)
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accent)
                            .kansoMechaAccents(accentActive = true, showCornerBrackets = true, showLedPip = false),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            color = colors.onAccent,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            // Bottom Recessed Drag Handle & Live Package Readout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoTactileClickable(onClick = onClose)
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.inkSecondary.copy(alpha = 0.42f))
                )
                Text(
                    text = "DRAWER // ${filteredApps.size} VISIBLE // ${visibleApps.size} PACKAGES INDEXED",
                    color = colors.inkSecondary.copy(alpha = 0.78f),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    letterSpacing = 0.9.sp
                )
            }
        }
    }

    // Long-Press App Context Sheet (100% existing actions preserved)
    contextMenuApp?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { contextMenuApp = null },
            containerColor = colors.surfaceRaised
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = app.label,
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${app.subsystemTag} // ${app.packageName}",
                    color = colors.inkSecondary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                DrawerContextAction(Icons.AutoMirrored.Rounded.AddToHomeScreen, "Add to Home Workspace") {
                    viewModel.addAppToHome(app, pageIndex = SurfacePage.WORKSPACE)
                    contextMenuApp = null
                }
                DrawerContextAction(Icons.Rounded.PushPin, "Pin to Dock") {
                    pinToDockApp = app
                    contextMenuApp = null
                }
                DrawerContextAction(Icons.Rounded.Palette, "Change Icon (Per-App Override)") {
                    iconPickerApp = app
                    contextMenuApp = null
                }
                DrawerContextAction(Icons.Rounded.VisibilityOff, "Hide App") {
                    viewModel.hideApp(app.packageName)
                    contextMenuApp = null
                }
                DrawerContextAction(Icons.Rounded.Info, "App Info") {
                    viewModel.appRepository.openAppInfo(app.packageName)
                    contextMenuApp = null
                }
                DrawerContextAction(Icons.Rounded.DeleteOutline, "Uninstall (via App Info)", tintAccent = true) {
                    viewModel.appRepository.requestUninstall(app.packageName)
                    contextMenuApp = null
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Pin to Dock Slot Picker
    pinToDockApp?.let { app ->
        Dialog(onDismissRequest = { pinToDockApp = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kansoNeumorphic(raised = true, cornerRadius = 24.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pin ${app.label} to Dock",
                    color = colors.inkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (slot in 0 until settings.dockSlotCount) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.darkModuleBg)
                                .kansoTactileClickable {
                                    viewModel.pinAppToDock(app, slot)
                                    pinToDockApp = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${slot + 1}",
                                color = colors.accent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Per-App Custom Icon Override Dialog
    iconPickerApp?.let { app ->
        PerAppIconOverrideDialog(
            app = app,
            installedPacks = installedPacks,
            iconPackManager = viewModel.iconPackManager,
            onSelectIcon = { packPkg, drawableName ->
                viewModel.setPerAppIconOverride(app.componentKey, packPkg, drawableName)
                iconPickerApp = null
            },
            onResetDefault = {
                viewModel.clearPerAppIconOverride(app.componentKey)
                iconPickerApp = null
            },
            onDismiss = { iconPickerApp = null }
        )
    }
}

@Composable
private fun DrawerContextAction(
    icon: ImageVector,
    label: String,
    tintAccent: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
            .kansoTactileClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (tintAccent) colors.accent else colors.inkPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = if (tintAccent) colors.accent else colors.inkPrimary,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AlphabetScrubberRail(
    highlightedLetter: Char,
    onLetterSelected: (Char) -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKansoColors.current
    var railHeightPx by remember { mutableIntStateOf(1) }

    fun letterForY(y: Float): Char {
        val fraction = (y / railHeightPx.coerceAtLeast(1)).coerceIn(0f, 0.999f)
        val idx = (fraction * ALPHABET.size).toInt().coerceIn(0, ALPHABET.lastIndex)
        return ALPHABET[idx]
    }

    Column(
        modifier = modifier
            .testTag("drawer_az_scrubber")
            .width(24.dp)
            .fillMaxHeight()
            .kansoNeumorphic(raised = false, cornerRadius = 12.dp, surfaceColor = colors.surfaceInset)
            .padding(vertical = 6.dp)
            .onSizeChanged { railHeightPx = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> onLetterSelected(letterForY(offset.y)) },
                    onVerticalDrag = { change, _ -> onLetterSelected(letterForY(change.position.y)) },
                    onDragEnd = onScrubEnd,
                    onDragCancel = onScrubEnd
                )
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ALPHABET.forEach { ch ->
            val isHighlighted = ch == highlightedLetter
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 13.dp)
                    .clip(CircleShape)
                    .background(if (isHighlighted) colors.accent else Color.Transparent)
                    .clickable { onLetterSelected(ch) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ch.toString(),
                    color = if (isHighlighted) colors.onAccent else colors.inkSecondary.copy(alpha = 0.72f),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun PerAppIconOverrideDialog(
    app: InstalledApp,
    installedPacks: List<IconPackInfo>,
    iconPackManager: IconPackManager,
    onSelectIcon: (String, String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKansoColors.current
    var selectedPack by remember { mutableStateOf(installedPacks.firstOrNull()) }
    var drawableNames by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(selectedPack) {
        drawableNames = selectedPack?.let { iconPackManager.getPackDrawableNames(it.packageName, 60) }.orEmpty()
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = true, cornerRadius = 26.dp)
                .padding(20.dp)
        ) {
            Text(
                text = "Icon for ${app.label}",
                color = colors.inkPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (installedPacks.isEmpty()) {
                Text(
                    text = "No third-party icon packs detected. Install any ADW/Nova/Apex icon pack from Google Play in Settings, or switch Kanso built-in icon style.",
                    color = colors.inkSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    installedPacks.take(3).forEach { pack ->
                        val active = selectedPack?.packageName == pack.packageName
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (active) colors.accent else colors.surfaceInset)
                                .kansoTactileClickable { selectedPack = pack }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pack.label,
                                color = if (active) colors.onAccent else colors.inkPrimary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 220.dp)
                ) {
                    items(drawableNames) { name ->
                        val packPkg = selectedPack?.packageName.orEmpty()
                        var bmp by remember(packPkg, name) { mutableStateOf<ImageBitmap?>(null) }
                        LaunchedEffect(packPkg, name) {
                            bmp = iconPackManager.loadDrawablePreview(packPkg, name)
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .kansoNeumorphic(raised = false, cornerRadius = 14.dp)
                                .kansoTactileClickable { onSelectIcon(packPkg, name) },
                            contentAlignment = Alignment.Center
                        ) {
                            bmp?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = name,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .kansoNeumorphic(raised = false, cornerRadius = 18.dp)
                        .kansoTactileClickable(onClick = onResetDefault)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Reset Default", color = colors.accent, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .kansoNeumorphic(raised = false, cornerRadius = 18.dp)
                        .kansoTactileClickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Close", color = colors.inkPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}
