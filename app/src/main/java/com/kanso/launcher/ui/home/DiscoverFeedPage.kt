package com.kanso.launcher.ui.home

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import com.kanso.launcher.R
import com.kanso.launcher.data.apps.InstalledApp
import com.kanso.launcher.data.db.IconOverrideEntity
import com.kanso.launcher.data.discover.DiscoverArticle
import com.kanso.launcher.data.discover.DiscoverCategory
import com.kanso.launcher.data.discover.DiscoverFeedRepository
import com.kanso.launcher.data.discover.DiscoverImageLoader
import com.kanso.launcher.data.iconpacks.IconPackManager
import com.kanso.launcher.data.prefs.IconPackMode
import com.kanso.launcher.ui.theme.JetBrainsMonoFontFamily
import com.kanso.launcher.ui.theme.LocalKansoColors
import com.kanso.launcher.ui.theme.SpaceGroteskFontFamily
import com.kanso.launcher.ui.theme.kansoIconNeumorphic
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
    onReturnHome: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalKansoColors.current

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DiscoverCategory.TOP) }
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

    val filteredArticles = remember(articles, query) {
        val q = query.trim()
        if (q.isEmpty()) articles
        else articles.filter {
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

    val chipShape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .testTag("google_discover_page")
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DISCOVER FEED",
                    color = colors.inkPrimary,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "NATIVE STREAM // LIVE NEWS & SEARCH",
                    color = colors.accent,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .testTag("discover_refresh_btn")
                        .size(44.dp)
                        .kansoNeumorphic(raised = true, cornerRadius = 14.dp)
                        .kansoTactileClickable { refreshTrigger++ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh news feed",
                        tint = colors.accent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .testTag("discover_return_home")
                        .size(44.dp)
                        .kansoNeumorphic(
                            raised = true,
                            cornerRadius = 14.dp,
                            surfaceColor = colors.darkModuleBg
                        )
                        .kansoTactileClickable(onClick = onReturnHome),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Return to Home",
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .testTag("discover_search_bar")
                .fillMaxWidth()
                .height(52.dp)
                .kansoNeumorphic(raised = false, cornerRadius = 16.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.discover_search_hint),
                        color = colors.inkSecondary,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.5.sp
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
                        .size(34.dp)
                        .kansoIconNeumorphic(cornerRadius = 10.dp, shadowScale = 0.5f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable { query = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = Color(0xFFF2F3F5),
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .testTag("discover_voice_btn")
                        .size(36.dp)
                        .kansoIconNeumorphic(cornerRadius = 11.dp, shadowScale = 0.5f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.darkModuleBg)
                        .kansoTactileClickable(onClick = onVoiceSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice Search",
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

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
                                    iconSize = 48.dp,
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
                        .kansoIconNeumorphic(cornerRadius = 12.dp, shadowScale = 0.6f)
                        .clip(chipShape)
                        .background(colors.accent)
                        .kansoTactileClickable(onClick = submitWebSearch)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Search web for \"${query.trim()}\"",
                        color = colors.onAccent,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
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

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 3.dp, horizontal = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DiscoverCategory.entries, key = { it.name }) { cat ->
                val selected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .testTag("discover_cat_${cat.name.lowercase()}")
                        .kansoNeumorphic(
                            raised = !selected,
                            cornerRadius = 12.dp,
                            surfaceColor = if (selected) colors.accent else colors.surfaceRaised
                        )
                        .kansoTactileClickable { selectedCategory = cat }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = cat.label.uppercase(),
                        color = if (selected) colors.onAccent else colors.inkPrimary,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

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
                            text = "SYNCING ${selectedCategory.label.uppercase()} STREAM…",
                            color = colors.inkSecondary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                errorText != null && filteredArticles.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .kansoNeumorphic(raised = true, cornerRadius = 20.dp)
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
                                .kansoIconNeumorphic(cornerRadius = 12.dp, shadowScale = 0.6f)
                                .clip(chipShape)
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
                        contentPadding = PaddingValues(top = 4.dp, bottom = 22.dp, start = 2.dp, end = 2.dp),
                        modifier = Modifier
                            .testTag("discover_articles_list")
                            .fillMaxSize()
                    ) {
                        items(filteredArticles, key = { it.id }) { article ->
                            DiscoverArticleCard(
                                article = article,
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
private fun DiscoverArticleCard(
    article: DiscoverArticle,
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

    val bannerShape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .testTag("discover_article_${article.id}")
            .fillMaxWidth()
            .kansoNeumorphic(raised = true, cornerRadius = 20.dp)
            .kansoTactileClickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Wide 16:9 banner image header on every Discover article card
        Box(
            modifier = Modifier
                .testTag("discover_article_banner_${article.id}")
                .fillMaxWidth()
                .height(158.dp)
                .clip(bannerShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.darkModuleElevated,
                            colors.darkModuleBg
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    color = colors.inkSecondary.copy(alpha = 0.16f),
                    shape = bannerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Stylized publisher fallback banner while loading or if remote host has no image
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

            // Subtle bottom gradient vignette + source pill badge overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = article.source.uppercase(),
                        color = colors.onAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = article.pubDate,
                    color = Color.White.copy(alpha = 0.90f),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.5.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = article.title,
                color = colors.inkPrimary,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                lineHeight = 19.5.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
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
