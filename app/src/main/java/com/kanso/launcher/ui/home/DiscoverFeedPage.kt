package com.kanso.launcher.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kanso.launcher.R
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.discover.DiscoverArticle
import com.kanso.launcher.data.discover.DiscoverCategory
import com.kanso.launcher.data.discover.DiscoverFeedRepository
import com.kanso.launcher.data.discover.DiscoverImageLoader
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.prefs.CachedWeather
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.data.telemetry.TelemetryState
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
import com.kanso.launcher.ui.theme.kansoMechaAccents
import com.kanso.launcher.ui.theme.kansoNeumorphic
import com.kanso.launcher.ui.theme.kansoTactileClickable
import com.kanso.launcher.ui.widgets.KansoAppIcon

@Composable
fun KansoDiscoverFeedPage(
    apps: List<InstalledApp>,
    iconPackManager: IconPackManager,
    iconPackMode: IconPackMode,
    selectedIconPackPackage: String,
    overrides: Map<String, IconOverrideEntity>,
    onLaunchApp: (String, String) -> Unit,
    onVoiceSearch: () -> Unit,
    onReturnHome: () -> Unit,
    weather: CachedWeather = CachedWeather(),
    telemetry: TelemetryState = TelemetryState(),
    savedUrls: Set<String> = emptySet(),
    onToggleBookmark: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalKansoColors.current

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DiscoverCategory.TOP) }
    var showSavedOnly by remember { mutableStateOf(false) }
    var articles by remember { mutableStateOf<List<DiscoverArticle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedCategory, refreshTrigger) {
        isLoading = true
        errorText = null
        val result = DiscoverFeedRepository.fetchCategory(
            category = selectedCategory,
            forceRefresh = refreshTrigger > 0
        )
        result.onSuccess { list ->
            articles = list
            errorText = if (list.isEmpty()) "No headlines available right now." else null
        }.onFailure { err ->
            errorText = err.message ?: "Unable to reach news feed. Check network connection."
        }
        isLoading = false
    }

    val matchingApps = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else apps.filter {
            it.label.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
        }.take(8)
    }

    val filteredArticles = remember(articles, query, showSavedOnly, savedUrls) {
        val base = if (showSavedOnly) {
            articles.filter { it.link in savedUrls }
        } else {
            articles
        }
        val q = query.trim()
        if (q.isEmpty()) base
        else base.filter {
            it.title.contains(q, ignoreCase = true) || it.source.contains(q, ignoreCase = true)
        }
    }

    val submitWebSearch = {
        val q = query.trim()
        if (q.isNotEmpty()) {
            val url = "https://www.google.com/search?q=${Uri.encode(q)}"
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    val dateLabel = remember(telemetry.clock.formattedDate) {
        telemetry.clock.formattedDate.removePrefix("Today ").uppercase()
    }

    Column(
        modifier = Modifier
            .testTag("google_discover_page")
            .fillMaxSize()
            .background(colors.canvasBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // Stitch Editorial Masthead
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DISCOVER // DISPATCHES • $dateLabel",
                    color = colors.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "THE DAILY FEED",
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.5).sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .testTag("discover_saved_filter_btn")
                        .size(40.dp)
                        .kansoNeumorphic(
                            raised = !showSavedOnly,
                            cornerRadius = 20.dp,
                            surfaceColor = if (showSavedOnly) colors.accent else colors.surfaceRaised
                        )
                        .kansoTactileClickable { showSavedOnly = !showSavedOnly },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (showSavedOnly) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Saved dispatches",
                        tint = if (showSavedOnly) colors.onAccent else colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .testTag("discover_refresh_btn")
                        .size(40.dp)
                        .kansoNeumorphic(raised = true, cornerRadius = 20.dp)
                        .kansoTactileClickable { refreshTrigger++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh news feed",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .testTag("discover_return_home")
                        .size(40.dp)
                        .kansoNeumorphic(
                            raised = true,
                            cornerRadius = 20.dp,
                            surfaceColor = colors.darkModuleBg
                        )
                        .kansoTactileClickable(onClick = onReturnHome),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Return to Home",
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Live Recessed Weather + System Telemetry Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .kansoNeumorphic(raised = false, cornerRadius = 15.dp, surfaceColor = colors.surfaceInset)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${weather.cityName.uppercase()} ${weather.temperatureC}°C • ${weather.humidityPercent}% RH",
                    color = colors.inkPrimary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${telemetry.batteryPercent}% BAT // ${telemetry.networkSpeedValue} ${telemetry.networkSpeedUnit}",
                color = colors.secondaryTelemetry,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }

        // Recessed Command & Search Bar
        Row(
            modifier = Modifier
                .testTag("discover_search_bar")
                .fillMaxWidth()
                .height(48.dp)
                .kansoNeumorphic(raised = false, cornerRadius = 16.dp, surfaceColor = colors.surfaceInset)
                .border(1.dp, colors.trackMuted.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.discover_search_hint),
                        color = colors.inkSecondary.copy(alpha = 0.78f),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = colors.inkPrimary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitWebSearch() }),
                    modifier = Modifier
                        .testTag("discover_search_input")
                        .fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .testTag("discover_clear_search")
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable { query = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .testTag("discover_voice_btn")
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable(onClick = onVoiceSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice Search",
                        tint = colors.accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Live App Match Strip + Web Search Action when typing
        if (query.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (matchingApps.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(matchingApps, key = { it.componentKey }) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .kansoTactileClickable {
                                        onLaunchApp(app.packageName, app.activityName)
                                    }
                            ) {
                                KansoAppIcon(
                                    packageName = app.packageName,
                                    activityName = app.activityName,
                                    contentDescription = app.label,
                                    iconPackManager = iconPackManager,
                                    iconPackMode = iconPackMode,
                                    selectedIconPackPackage = selectedIconPackPackage,
                                    iconOverride = overrides[app.componentKey],
                                    iconSize = 46.dp,
                                    cornerRadius = 13.dp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = app.label,
                                    color = colors.inkPrimary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .testTag("discover_web_search_submit")
                        .fillMaxWidth()
                        .kansoIconNeumorphic(cornerRadius = 14.dp, shadowScale = 0.5f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accent)
                        .kansoTactileClickable(onClick = submitWebSearch)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SEARCH WEB FOR \"${query.trim().uppercase()}\"",
                        color = colors.onAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Horizontal Topic Channels Pill Rail
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DiscoverCategory.entries, key = { it.name }) { cat ->
                val selected = !showSavedOnly && cat == selectedCategory
                val label = if (cat == DiscoverCategory.TOP) "FOR YOU" else cat.label.uppercase()
                Box(
                    modifier = Modifier
                        .testTag("discover_cat_${cat.name.lowercase()}")
                        .clip(CircleShape)
                        .background(if (selected) colors.accent else colors.surfaceRaised)
                        .border(
                            width = 1.dp,
                            color = if (selected) colors.accent else colors.trackMuted.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
                        .kansoTactileClickable {
                            showSavedOnly = false
                            selectedCategory = cat
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) colors.onAccent else colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp
                    )
                }
            }
            item(key = "cat_saved") {
                Box(
                    modifier = Modifier
                        .testTag("discover_cat_saved")
                        .clip(CircleShape)
                        .background(if (showSavedOnly) colors.accent else colors.surfaceRaised)
                        .border(
                            width = 1.dp,
                            color = if (showSavedOnly) colors.accent else colors.trackMuted.copy(alpha = 0.45f),
                            shape = CircleShape
                        )
                        .kansoTactileClickable { showSavedOnly = !showSavedOnly }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "SAVED (${savedUrls.size})",
                        color = if (showSavedOnly) colors.onAccent else colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp
                    )
                }
            }
        }

        // Editorial Dispatch Cards Feed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoading && filteredArticles.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = colors.accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "SYNCING ${selectedCategory.label.uppercase()} DISPATCHES…",
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                showSavedOnly && filteredArticles.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = "No saved dispatches in this channel",
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap SAVE on any headline card to bookmark it for later.",
                            color = colors.inkSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                errorText != null && filteredArticles.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Newspaper,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            text = errorText ?: "Feed unavailable",
                            color = colors.inkPrimary,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .testTag("discover_retry_btn")
                                .clip(CircleShape)
                                .background(colors.accent)
                                .kansoTactileClickable { refreshTrigger++ }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = "RETRY FEED",
                                color = colors.onAccent,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 22.dp),
                        modifier = Modifier
                            .testTag("discover_articles_list")
                            .fillMaxSize()
                    ) {
                        itemsIndexed(filteredArticles, key = { _, it -> it.id }) { index, article ->
                            val accentColor = when {
                                index == 0 -> colors.accent
                                index == 1 -> colors.secondaryTelemetry
                                else -> colors.inkSecondary.copy(alpha = 0.5f)
                            }
                            StitchEditorialArticleCard(
                                article = article,
                                accentStripColor = accentColor,
                                isBookmarked = article.link in savedUrls,
                                onToggleBookmark = { onToggleBookmark(article.link) },
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, article.link.toUri()).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
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
private fun StitchEditorialArticleCard(
    article: DiscoverArticle,
    accentStripColor: Color,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit
) {
    val colors = LocalKansoColors.current
    var bannerBitmap by remember(article.id) {
        mutableStateOf<ImageBitmap?>(DiscoverImageLoader.getCached(article.id)?.asImageBitmap())
    }
    var attemptedLoad by remember(article.id) { mutableStateOf(bannerBitmap != null) }

    LaunchedEffect(article.id) {
        if (bannerBitmap == null) {
            val loaded = DiscoverImageLoader.loadBanner(article)
            bannerBitmap = loaded?.asImageBitmap()
            attemptedLoad = true
        }
    }

    val bannerShape = RoundedCornerShape(15.dp)

    Column(
        modifier = Modifier
            .testTag("discover_article_${article.id}")
            .fillMaxWidth()
            .kansoNeumorphic(raised = true, cornerRadius = 22.dp)
            .kansoMechaAccents(accentActive = accentStripColor == colors.accent, showCornerBrackets = true, showLedPip = false)
            .kansoTactileClickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Wide 16:9 Banner Image Header on every article card
        Box(
            modifier = Modifier
                .testTag("discover_article_banner_${article.id}")
                .fillMaxWidth()
                .height(162.dp)
                .clip(bannerShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(colors.darkModuleElevated, colors.darkModuleBg)
                    )
                )
                .border(0.8.dp, colors.inkSecondary.copy(alpha = 0.18f), bannerShape),
            contentAlignment = Alignment.Center
        ) {
            DiscoverBannerFallback(
                source = article.source,
                isLoading = !attemptedLoad
            )

            val bmp = bannerBitmap
            androidx.compose.animation.AnimatedVisibility(
                visible = bmp != null,
                enter = fadeIn(animationSpec = tween(220))
            ) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = article.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentStripColor.copy(alpha = 0.94f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = article.source.uppercase(),
                        color = if (accentStripColor == colors.accent) colors.onAccent else Color(0xFF0A0C10),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = article.pubDate.uppercase(),
                    color = Color.White.copy(alpha = 0.90f),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.5.sp
                )
            }
        }

        // Left Accent Bar + Editorial Headline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(accentStripColor)
            )
            Text(
                text = article.title,
                color = colors.inkPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Action Row: READ DISPATCH + Persistent SAVE Bookmark
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.accent)
                    .kansoTactileClickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "READ DISPATCH",
                    color = colors.onAccent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.6.sp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(13.dp)
                )
            }

            Row(
                modifier = Modifier
                    .testTag("discover_bookmark_${article.id}")
                    .clip(CircleShape)
                    .background(if (isBookmarked) colors.accent.copy(alpha = 0.16f) else colors.surfaceInset)
                    .border(
                        width = 1.dp,
                        color = if (isBookmarked) colors.accent else colors.trackMuted.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .kansoTactileClickable(onClick = onToggleBookmark)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Save dispatch",
                    tint = if (isBookmarked) colors.accent else colors.inkSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isBookmarked) "SAVED" else "SAVE",
                    color = if (isBookmarked) colors.accent else colors.inkSecondary,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp
                )
            }
        }
    }
}

@Composable
private fun DiscoverBannerFallback(
    source: String,
    isLoading: Boolean
) {
    val colors = LocalKansoColors.current
    val initials = remember(source) {
        source.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "NW" }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.24f),
                        colors.darkModuleBg,
                        colors.accent.copy(alpha = 0.14f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
            val step = 22.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += step
            }
        }
        if (isLoading) {
            CircularProgressIndicator(
                color = colors.accent.copy(alpha = 0.85f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = initials,
                color = colors.accent.copy(alpha = 0.48f),
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                letterSpacing = 2.sp
            )
        }
    }
}
