package com.kanso.launcher.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.kanso.launcher.service.KansoAccessibilityService
import java.util.Locale

data class RomEnvironmentInfo(
    val manufacturer: String,
    val model: String,
    val isChineseRom: Boolean,
    val regionLabel: String
)

object RomDetector {

    private val chineseBrands = setOf("honor", "huawei", "xiaomi", "redmi", "poco", "oppo", "vivo", "iqoo", "realme", "meizu")

    fun detect(): RomEnvironmentInfo {
        val countryProp = getSystemProperty("ro.hw.country")?.lowercase(Locale.ROOT)
        val regionProp = getSystemProperty("ro.product.locale.region")?.lowercase(Locale.ROOT)
        val miuiRegion = getSystemProperty("ro.miui.region")?.lowercase(Locale.ROOT)
        val localeProp = getSystemProperty("ro.product.locale")?.lowercase(Locale.ROOT)
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val lowerBrand = manufacturer.lowercase(Locale.ROOT)

        val isChinaRegion = countryProp == "cn" ||
            regionProp == "cn" ||
            miuiRegion == "cn" ||
            localeProp?.contains("zh-cn") == true ||
            localeProp?.contains("zh_cn") == true
        val isChineseBrand = chineseBrands.any { lowerBrand.contains(it) }
        val isCnRom = isChinaRegion && isChineseBrand

        val regionLabel = when {
            isCnRom -> "CN OEM ROM (${manufacturer.uppercase(Locale.ROOT)})"
            isChineseBrand -> "GLOBAL OEM (${manufacturer.uppercase(Locale.ROOT)})"
            else -> "GLOBAL ANDROID (${manufacturer.uppercase(Locale.ROOT)})"
        }

        return RomEnvironmentInfo(
            manufacturer = manufacturer,
            model = Build.MODEL.orEmpty(),
            isChineseRom = isCnRom,
            regionLabel = regionLabel
        )
    }

    fun isChineseRom(): Boolean = detect().isChineseRom

    @SuppressLint("PrivateApi")
    private fun getSystemProperty(key: String): String? {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java)
            (getMethod.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun buildDefaultLauncherIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun requestDefaultLauncher(context: Context) {
        val intent = buildDefaultLauncherIntent(context)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { context.startActivity(intent); true }.getOrDefault(false)
        if (!launched) {
            val fallback = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun isDefaultHomeLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return true
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expected = ComponentName(context, KansoAccessibilityService::class.java).flattenToString()
        val shortExpected = ComponentName(context, KansoAccessibilityService::class.java).flattenToShortString()
        return enabled.contains(expected, ignoreCase = true) ||
            enabled.contains(shortExpected, ignoreCase = true) ||
            KansoAccessibilityService.isRunning.value
    }

    @Suppress("DEPRECATION")
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasWriteSecureSettings(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
