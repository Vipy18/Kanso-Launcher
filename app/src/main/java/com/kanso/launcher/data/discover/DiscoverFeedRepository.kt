package com.kanso.launcher.data.discover

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

enum class DiscoverCategory(
    val label: String,
    val rssUrl: String,
    val fallbackRssUrl: String,
    val defaultSource: String
) {
    TOP(
        label = "Top",
        rssUrl = "https://feeds.bbci.co.uk/news/rss.xml",
        fallbackRssUrl = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en",
        defaultSource = "BBC News"
    ),
    TECH(
        label = "Tech",
        rssUrl = "https://feeds.bbci.co.uk/news/technology/rss.xml",
        fallbackRssUrl = "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en",
        defaultSource = "BBC Tech"
    ),
    SCIENCE(
        label = "Science",
        rssUrl = "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml",
        fallbackRssUrl = "https://news.google.com/rss/headlines/section/topic/SCIENCE?hl=en-US&gl=US&ceid=US:en",
        defaultSource = "BBC Science"
    ),
    WORLD(
        label = "World",
        rssUrl = "https://feeds.bbci.co.uk/news/world/rss.xml",
        fallbackRssUrl = "https://news.google.com/rss/headlines/section/topic/WORLD?hl=en-US&gl=US&ceid=US:en",
        defaultSource = "BBC World"
    ),
    BUSINESS(
        label = "Business",
        rssUrl = "https://feeds.bbci.co.uk/news/business/rss.xml",
        fallbackRssUrl = "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en-US&gl=US&ceid=US:en",
        defaultSource = "BBC Business"
    )
}

data class DiscoverArticle(
    val id: String,
    val title: String,
    val source: String,
    val link: String,
    val pubDate: String,
    val imageUrl: String? = null
)

private data class CachedCategoryFeed(
    val fetchedAtMs: Long,
    val articles: List<DiscoverArticle>
)

object DiscoverFeedRepository {

    private const val CACHE_TTL_MS = 10 * 60 * 1000L
    private val cache = ConcurrentHashMap<DiscoverCategory, CachedCategoryFeed>()
    private val imgTagRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    suspend fun fetchCategory(
        category: DiscoverCategory,
        forceRefresh: Boolean = false
    ): Result<List<DiscoverArticle>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = cache[category]
            if (cached != null && now - cached.fetchedAtMs < CACHE_TTL_MS && cached.articles.isNotEmpty()) {
                return@withContext Result.success(cached.articles)
            }
        }

        val primary = fetchUrl(category.rssUrl, category.defaultSource)
        val result = if (primary.isSuccess && primary.getOrNull()?.isNotEmpty() == true) {
            primary
        } else {
            fetchUrl(category.fallbackRssUrl, "News")
        }

        result.onSuccess { parsed ->
            cache[category] = CachedCategoryFeed(fetchedAtMs = now, articles = parsed)
        }
    }

    private fun fetchUrl(urlString: String, defaultSource: String): Result<List<DiscoverArticle>> = runCatching {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; KansoLauncher/1.0)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("Feed HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseRssXml(body, defaultSource).take(22)
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseRssXml(xml: String, defaultSource: String): List<DiscoverArticle> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        val out = ArrayList<DiscoverArticle>()
        var inItem = false
        var title = ""
        var link = ""
        var pubDate = ""
        var source = ""
        var description = ""
        var imageUrl: String? = null
        var currentTag = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = (parser.name ?: "").lowercase()
                    if (tag == "item") {
                        inItem = true
                        title = ""
                        link = ""
                        pubDate = ""
                        source = ""
                        description = ""
                        imageUrl = null
                    } else if (inItem) {
                        currentTag = tag
                        if (tag == "media:thumbnail" || tag == "media:content" || tag == "enclosure") {
                            val candidate = parser.getAttributeValue(null, "url")?.trim()
                            val mimeType = parser.getAttributeValue(null, "type")?.lowercase()
                            val isImage = mimeType == null || mimeType.startsWith("image/") ||
                                candidate?.contains(".jpg", ignoreCase = true) == true ||
                                candidate?.contains(".jpeg", ignoreCase = true) == true ||
                                candidate?.contains(".png", ignoreCase = true) == true ||
                                candidate?.contains(".webp", ignoreCase = true) == true ||
                                tag.startsWith("media:")
                            if (isImage && !candidate.isNullOrBlank() && imageUrl == null) {
                                imageUrl = normalizeImageUrl(candidate)
                            }
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text ?: ""
                        when (currentTag) {
                            "title" -> title += text
                            "link" -> link += text
                            "pubdate" -> pubDate += text
                            "source" -> source += text
                            "description", "content:encoded" -> description += text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val tag = (parser.name ?: "").lowercase()
                    if (tag == "item" && inItem) {
                        val cleanTitle = title.trim()
                        val cleanLink = link.trim()
                        if (cleanTitle.isNotEmpty() && cleanLink.isNotEmpty()) {
                            val splitIdx = cleanTitle.lastIndexOf(" - ")
                            val headline = if (splitIdx > 12) cleanTitle.substring(0, splitIdx).trim() else cleanTitle
                            val resolvedSource = source.trim().ifBlank {
                                if (splitIdx > 12) cleanTitle.substring(splitIdx + 3).trim() else defaultSource
                            }
                            val resolvedImg = imageUrl ?: extractImageFromHtml(description)
                            out.add(
                                DiscoverArticle(
                                    id = "${out.size}_${cleanLink.hashCode()}",
                                    title = headline,
                                    source = resolvedSource,
                                    link = cleanLink,
                                    pubDate = formatShortDate(pubDate.trim()),
                                    imageUrl = resolvedImg
                                )
                            )
                        }
                        inItem = false
                    }
                    currentTag = ""
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun extractImageFromHtml(html: String): String? {
        if (html.isBlank()) return null
        val match = imgTagRegex.find(html) ?: return null
        return normalizeImageUrl(match.groupValues[1])
    }

    private fun normalizeImageUrl(raw: String): String? {
        val clean = raw.trim().replace("&amp;", "&")
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) return null
        return if (clean.startsWith("http://")) clean.replaceFirst("http://", "https://") else clean
    }

    private fun formatShortDate(raw: String): String {
        if (raw.isBlank()) return "LIVE"
        val parts = raw.split(" ").filter { it.isNotBlank() }
        return if (parts.size >= 5) {
            val timeShort = parts[4].take(5)
            "${parts[1]} ${parts[2].uppercase()} • $timeShort"
        } else {
            raw.take(18).uppercase()
        }
    }
}

/**
 * Bounded LRU bitmap loader and OpenGraph image resolver for Discover article wide banners.
 * Downscales decoded bitmaps to ~680px width to keep launcher memory footprint small.
 */
object DiscoverImageLoader {

    private const val MAX_BITMAP_CACHE_ENTRIES = 24
    private const val TARGET_BANNER_WIDTH_PX = 680

    private val bitmapCache = object : LruCache<String, Bitmap>(MAX_BITMAP_CACHE_ENTRIES) {}
    private val resolvedUrlCache = ConcurrentHashMap<String, String>()
    private val ogImageRegex = Regex(
        """<meta[^>]+(?:property|name)=["'](?:og:image|twitter:image)["'][^>]+content=["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )
    private val ogImageAltRegex = Regex(
        """<meta[^>]+content=["']([^"']+)["'][^>]+(?:property|name)=["'](?:og:image|twitter:image)["']""",
        RegexOption.IGNORE_CASE
    )

    fun getCached(articleId: String): Bitmap? = bitmapCache.get(articleId)

    suspend fun loadBanner(article: DiscoverArticle): Bitmap? = withContext(Dispatchers.IO) {
        bitmapCache.get(article.id)?.let { return@withContext it }

        val candidateUrl = article.imageUrl
            ?: resolvedUrlCache[article.id]?.takeIf { it.isNotBlank() }
            ?: resolveOgImage(article.link)?.also { resolvedUrlCache[article.id] = it }
            ?: return@withContext null

        val upgradedUrl = upgradeThumbnailUrl(candidateUrl)
        val bitmap = downloadBitmap(upgradedUrl)
            ?: if (upgradedUrl != candidateUrl) downloadBitmap(candidateUrl) else null

        if (bitmap != null) {
            bitmapCache.put(article.id, bitmap)
        }
        bitmap
    }

    private fun upgradeThumbnailUrl(url: String): String =
        if (url.contains("ichef.bbci.co.uk") && url.contains("/240/")) {
            url.replace("/240/", "/624/")
        } else {
            url
        }

    private fun resolveOgImage(articleUrl: String): String? = runCatching {
        if (articleUrl.contains("news.google.com")) return@runCatching null
        val conn = (URL(articleUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4500
            readTimeout = 4500
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; KansoLauncher/1.0)")
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val buffer = CharArray(24 * 1024)
            val readCount = conn.inputStream.bufferedReader().use { it.read(buffer) }
            if (readCount <= 0) return@runCatching null
            val head = String(buffer, 0, readCount)
            val raw = ogImageRegex.find(head)?.groupValues?.getOrNull(1)
                ?: ogImageAltRegex.find(head)?.groupValues?.getOrNull(1)
                ?: return@runCatching null
            val clean = raw.trim().replace("&amp;", "&")
            if (clean.startsWith("https://")) clean
            else if (clean.startsWith("http://")) clean.replaceFirst("http://", "https://")
            else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun downloadBitmap(urlString: String): Bitmap? = runCatching {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6000
            readTimeout = 6000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; KansoLauncher/1.0)")
        }
        try {
            if (conn.responseCode !in 200..299) return@runCatching null
            val bytes = conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var total = 0
                val maxBytes = 2 * 1024 * 1024
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    total += n
                    if (total > maxBytes) return@use null
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            } ?: return@runCatching null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= TARGET_BANNER_WIDTH_PX) {
                sampleSize *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}
