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

    private val chineseBrands = setOf(
        "honor", "huawei", "xiaomi", "redmi", "poco",
        "oppo", "oneplus", "realme", "vivo", "iqoo", "meizu"
    )

    fun detect(): RomEnvironmentInfo {
        return classifyRom(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            countryProp = getSystemProperty("ro.hw.country"),
            regionProp = getSystemProperty("ro.product.locale.region"),
            miuiRegion = getSystemProperty("ro.miui.region"),
            localeProp = getSystemProperty("ro.product.locale"),
            oplusRegion = getSystemProperty("ro.vendor.oplus.regionmark")
                ?: getSystemProperty("persist.sys.oppo.region"),
            vivoOverseas = getSystemProperty("ro.vivo.product.overseas")
        )
    }

    internal fun classifyRom(
        manufacturer: String,
        model: String,
        countryProp: String? = null,
        regionProp: String? = null,
        miuiRegion: String? = null,
        localeProp: String? = null,
        oplusRegion: String? = null,
        vivoOverseas: String? = null
    ): RomEnvironmentInfo {
        val lowerBrand = manufacturer.lowercase(Locale.ROOT)
        val lowerCountry = countryProp?.lowercase(Locale.ROOT)
        val lowerRegion = regionProp?.lowercase(Locale.ROOT)
        val lowerMiui = miuiRegion?.lowercase(Locale.ROOT)
        val lowerLocale = localeProp?.lowercase(Locale.ROOT)
        val lowerOplus = oplusRegion?.lowercase(Locale.ROOT)
        val lowerVivoOverseas = vivoOverseas?.lowercase(Locale.ROOT)

        val isChinaRegion = lowerCountry == "cn" ||
            lowerRegion == "cn" ||
            lowerMiui == "cn" ||
            lowerOplus == "cn" ||
            lowerVivoOverseas == "no" ||
            lowerLocale?.contains("zh-cn") == true ||
            lowerLocale?.contains("zh_cn") == true

        val isChineseBrand = chineseBrands.any { lowerBrand.contains(it) }
        val isCnRom = isChinaRegion && isChineseBrand

        val upperBrand = manufacturer.ifBlank { "ANDROID" }.uppercase(Locale.ROOT)
        val regionLabel = when {
            isCnRom -> "CN OEM ROM ($upperBrand)"
            isChineseBrand -> "GLOBAL OEM ($upperBrand)"
            else -> "GLOBAL ANDROID ($upperBrand)"
        }

        return RomEnvironmentInfo(
            manufacturer = manufacturer,
            model = model,
            isChineseRom = isCnRom,
            regionLabel = regionLabel
        )
    }

    fun isChineseRom(): Boolean = detect().isChineseRom

    fun isHonorOrHuawei(manufacturer: String = Build.MANUFACTURER.orEmpty()): Boolean {
        val lower = manufacturer.lowercase(Locale.ROOT)
        return lower.contains("honor") || lower.contains("huawei")
    }

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

    fun openAppDetailsSettings(context: Context, packageName: String = context.packageName) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:$packageName".toUri()
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Opens OEM App Launch / Auto-Start / Battery protection settings on Chinese ROMs
     * (Honor MagicOS, Huawei HarmonyOS/EMUI, Xiaomi HyperOS/MIUI, OPPO ColorOS, Vivo OriginOS)
     * so background power managers do not kill KansoAccessibilityService or KansoLifecycleService.
     */
    fun openOemBatteryAndLaunchSettings(context: Context) {
        val candidates = listOf(
            ComponentName("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
        )
        for (component in candidates) {
            val intent = Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolved != null && runCatching { context.startActivity(intent); true }.getOrDefault(false)) {
                return
            }
        }
        val batteryOptIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!runCatching { context.startActivity(batteryOptIntent); true }.getOrDefault(false)) {
            openAppDetailsSettings(context)
        }
    }
}
