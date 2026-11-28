package com.kanso.launcher.data.iconpacks

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.kanso.launcher.data.prefs.IconPackMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

data class IconPackInfo(
    val packageName: String,
    val label: String
)

private data class ParsedIconPack(
    val packageName: String,
    val resources: Resources,
    val componentToDrawable: Map<String, String>,
    val iconBacks: List<String>,
    val iconMask: String?,
    val iconUpon: String?,
    val scale: Float,
    val allDrawableNames: List<String>
)

class IconPackManager(private val context: Context) {

    private val pm = context.packageManager

    private val iconCache = object : LruCache<String, ImageBitmap>(180) {}
    private val parsedPackCache = ConcurrentHashMap<String, ParsedIconPack>()

    companion object {
        private val ICON_PACK_INTENTS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme"
        )
        private const val TARGET_ICON_PX = 144
    }

    fun clearBitmapCache() {
        iconCache.evictAll()
    }

    suspend fun discoverInstalledIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, IconPackInfo>()
        for (action in ICON_PACK_INTENTS) {
            val resolved = runCatching {
                pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
            }.getOrDefault(emptyList())
            for (ri in resolved) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg == context.packageName || found.containsKey(pkg)) continue
                val label = ri.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                found[pkg] = IconPackInfo(packageName = pkg, label = label)
            }
        }
        found.values.sortedBy { it.label.lowercase() }
    }

    suspend fun getPackDrawableNames(packPackage: String, limit: Int = 80): List<String> =
        withContext(Dispatchers.IO) {
            loadIconPack(packPackage)?.allDrawableNames?.take(limit) ?: emptyList()
        }

    suspend fun loadPackPreviewBitmaps(packPackage: String, count: Int = 4): List<ImageBitmap> =
        withContext(Dispatchers.IO) {
            val pack = loadIconPack(packPackage) ?: return@withContext emptyList()
            pack.allDrawableNames.take(count).mapNotNull { name ->
                loadDrawableFromPack(pack, name)?.toSquareBitmap(TARGET_ICON_PX)?.asImageBitmap()
            }
        }

    suspend fun loadDrawablePreview(packPackage: String, drawableName: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val cacheKey = "raw|$packPackage|$drawableName"
            iconCache.get(cacheKey)?.let { return@withContext it }
            val pack = loadIconPack(packPackage) ?: return@withContext null
            val bmp = loadDrawableFromPack(pack, drawableName)
                ?.toSquareBitmap(TARGET_ICON_PX)
                ?.asImageBitmap()
            if (bmp != null) iconCache.put(cacheKey, bmp)
            bmp
        }

    suspend fun loadAppIcon(
        packageName: String,
        activityName: String,
        mode: IconPackMode,
        selectedPackPackage: String,
        accentColor: Color,
        inkColor: Color,
        overridePackPackage: String? = null,
        overrideDrawableName: String? = null
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val tintKey = when (mode) {
            IconPackMode.KANSO_MONOCHROME -> "mono_${inkColor.toArgb()}"
            IconPackMode.KANSO_ACCENT -> "acc_${accentColor.toArgb()}"
            IconPackMode.SYSTEM_DEFAULT -> "sys"
            IconPackMode.THIRD_PARTY -> "pack_$selectedPackPackage"
        }
        val overrideKey = if (!overridePackPackage.isNullOrBlank() && !overrideDrawableName.isNullOrBlank()) {
            "$overridePackPackage/$overrideDrawableName"
        } else {
            ""
        }
        val cacheKey = "$packageName/$activityName|$tintKey|$overrideKey"
        iconCache.get(cacheKey)?.let { return@withContext it }

        val resultBitmap: Bitmap? = when {
            !overridePackPackage.isNullOrBlank() && !overrideDrawableName.isNullOrBlank() -> {
                val pack = loadIconPack(overridePackPackage)
                pack?.let { loadDrawableFromPack(it, overrideDrawableName)?.toSquareBitmap(TARGET_ICON_PX) }
                    ?: loadSystemAppBitmap(packageName, activityName)
            }
            mode == IconPackMode.THIRD_PARTY && selectedPackPackage.isNotBlank() -> {
                loadFromThirdPartyPack(packageName, activityName, selectedPackPackage)
                    ?: loadSystemAppBitmap(packageName, activityName)
            }
            mode == IconPackMode.KANSO_MONOCHROME -> {
                loadSystemAppBitmap(packageName, activityName)?.let { tintMonochrome(it, inkColor.toArgb()) }
            }
            mode == IconPackMode.KANSO_ACCENT -> {
                loadSystemAppBitmap(packageName, activityName)?.let { tintMonochrome(it, accentColor.toArgb()) }
            }
            else -> loadSystemAppBitmap(packageName, activityName)
        }

        val imageBitmap = resultBitmap?.asImageBitmap()
        if (imageBitmap != null) {
            iconCache.put(cacheKey, imageBitmap)
        }
        imageBitmap
    }

    private fun loadSystemAppBitmap(packageName: String, activityName: String): Bitmap? {
        val drawable: Drawable = runCatching {
            if (activityName.isNotBlank()) {
                pm.getActivityIcon(android.content.ComponentName(packageName, activityName))
            } else {
                pm.getApplicationIcon(packageName)
            }
        }.getOrElse {
            runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
        } ?: return null
        return drawable.toSquareBitmap(TARGET_ICON_PX)
    }

    private fun loadFromThirdPartyPack(
        packageName: String,
        activityName: String,
        packPackage: String
    ): Bitmap? {
        val pack = loadIconPack(packPackage) ?: return null
        val fullKey = "ComponentInfo{$packageName/$activityName}"
        val exactDrawable = pack.componentToDrawable[fullKey]
            ?: pack.componentToDrawable.entries.firstOrNull { it.key.startsWith("ComponentInfo{$packageName/") }?.value

        if (exactDrawable != null) {
            val themed = loadDrawableFromPack(pack, exactDrawable)
            if (themed != null) return themed.toSquareBitmap(TARGET_ICON_PX)
        }

        // Unthemed fallback compositing with <iconback>, <iconmask> (DST_OUT), <iconupon>, and <scale>
        val original = loadSystemAppBitmap(packageName, activityName) ?: return null
        return compositeMaskedFallback(pack, original)
    }

    private fun compositeMaskedFallback(pack: ParsedIconPack, source: Bitmap): Bitmap {
        val size = TARGET_ICON_PX
        val out = createBitmap(size, size)
        val canvas = Canvas(out)

        // 1. Draw <iconback>
        if (pack.iconBacks.isNotEmpty()) {
            val backName = pack.iconBacks[(source.hashCode() and 0x7FFFFFFF) % pack.iconBacks.size]
            loadDrawableFromPack(pack, backName)?.let { backDrawable ->
                backDrawable.setBounds(0, 0, size, size)
                backDrawable.draw(canvas)
            }
        }

        // 2. Scale source icon onto offscreen layer and apply <iconmask> via DST_OUT
        val maskedLayer = createBitmap(size, size)
        val maskCanvas = Canvas(maskedLayer)
        val scaledDim = (size * pack.scale.coerceIn(0.55f, 1.0f)).roundToInt()
        val inset = (size - scaledDim) / 2
        val scaledSrc = source.scale(scaledDim, scaledDim)
        maskCanvas.drawBitmap(scaledSrc, inset.toFloat(), inset.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG))

        pack.iconMask?.let { maskName ->
            loadDrawableFromPack(pack, maskName)?.toSquareBitmap(size)?.let { maskBmp ->
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                }
                maskCanvas.drawBitmap(maskBmp, 0f, 0f, maskPaint)
            }
        }
        canvas.drawBitmap(maskedLayer, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))

        // 3. Draw <iconupon>
        pack.iconUpon?.let { uponName ->
            loadDrawableFromPack(pack, uponName)?.let { uponDrawable ->
                uponDrawable.setBounds(0, 0, size, size)
                uponDrawable.draw(canvas)
            }
        }
        return out
    }

    private fun tintMonochrome(source: Bitmap, targetColorArgb: Int): Bitmap {
        val out = createBitmap(source.width, source.height)
        val canvas = Canvas(out)

        // Desaturate + subtly boost contrast, then apply monochrome hardware tint
        val gray = createBitmap(source.width, source.height)
        val grayCanvas = Canvas(gray)
        val satMatrix = ColorMatrix().apply { setSaturation(0f) }
        val grayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(satMatrix)
        }
        grayCanvas.drawBitmap(source, 0f, 0f, grayPaint)

        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(targetColorArgb, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(gray, 0f, 0f, tintPaint)
        return out
    }

    @SuppressLint("DiscouragedApi")
    private fun loadIconPack(packPackage: String): ParsedIconPack? {
        parsedPackCache[packPackage]?.let { return it }
        val resources = runCatching { pm.getResourcesForApplication(packPackage) }.getOrNull()
            ?: return null

        val map = linkedMapOf<String, String>()
        val iconBacks = mutableListOf<String>()
        var iconMask: String? = null
        var iconUpon: String? = null
        var scale = 0.85f

        val xmlResId = resources.getIdentifier("appfilter", "xml", packPackage)
        var assetStream: InputStream? = null
        val xmlResourceParser = if (xmlResId != 0) {
            runCatching { resources.getXml(xmlResId) }.getOrNull()
        } else {
            null
        }
        val parser: XmlPullParser? = xmlResourceParser ?: runCatching {
            assetStream = resources.assets.open("appfilter.xml")
            XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
                .newPullParser().apply { setInput(assetStream, "UTF-8") }
        }.onFailure {
            runCatching { assetStream?.close() }
            assetStream = null
        }.getOrNull()

        if (parser != null) {
            try {
                runCatching {
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            when (parser.name) {
                                "item" -> {
                                    val comp = parser.getAttributeValue(null, "component")
                                    val draw = parser.getAttributeValue(null, "drawable")
                                    if (!comp.isNullOrBlank() && !draw.isNullOrBlank()) {
                                        map[comp] = draw
                                    }
                                }
                                "iconback" -> {
                                    for (i in 0 until parser.attributeCount) {
                                        val value = parser.getAttributeValue(i)
                                        if (!value.isNullOrBlank()) iconBacks.add(value)
                                    }
                                }
                                "iconmask" -> {
                                    iconMask = parser.getAttributeValue(null, "img1")
                                        ?: if (parser.attributeCount > 0) parser.getAttributeValue(0) else null
                                }
                                "iconupon" -> {
                                    iconUpon = parser.getAttributeValue(null, "img1")
                                        ?: if (parser.attributeCount > 0) parser.getAttributeValue(0) else null
                                }
                                "scale" -> {
                                    parser.getAttributeValue(null, "factor")?.toFloatOrNull()?.let { scale = it }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            } finally {
                runCatching { xmlResourceParser?.close() }
                runCatching { assetStream?.close() }
            }
        }

        val parsed = ParsedIconPack(
            packageName = packPackage,
            resources = resources,
            componentToDrawable = map,
            iconBacks = iconBacks,
            iconMask = iconMask,
            iconUpon = iconUpon,
            scale = scale,
            allDrawableNames = map.values.distinct()
        )
        parsedPackCache[packPackage] = parsed
        return parsed
    }

    @SuppressLint("DiscouragedApi")
    private fun loadDrawableFromPack(pack: ParsedIconPack, drawableName: String): Drawable? {
        val id = pack.resources.getIdentifier(drawableName, "drawable", pack.packageName)
        if (id == 0) return null
        return runCatching { pack.resources.getDrawable(id, null) }.getOrNull()
    }

    private fun Drawable.toSquareBitmap(sizePx: Int): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, sizePx, sizePx)
        draw(canvas)
        return bitmap
    }
}
