package com.kanso.launcher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val userHandle: UserHandle
) {
    val componentKey: String get() = "$packageName/$activityName"
    val sectionLetter: Char
        get() {
            val ch = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
            return if (ch in 'A'..'Z') ch else '#'
        }
}

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val packageManager = context.packageManager

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            refreshApps()
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            refreshApps()
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            refreshApps()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            refreshApps()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            refreshApps()
        }
    }

    init {
        runCatching { launcherApps.registerCallback(callback) }
        refreshApps()
    }

    fun refreshApps() {
        scope.launch {
            val list = withContext(Dispatchers.IO) { queryLaunchableApps() }
            _installedApps.value = list
        }
    }

    private fun queryLaunchableApps(): List<InstalledApp> {
        val selfPkg = context.packageName
        val user = Process.myUserHandle()
        val activities = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
        return activities
            .filter { it.applicationInfo.packageName != selfPkg }
            .map { info ->
                InstalledApp(
                    label = info.label?.toString()?.takeIf { it.isNotBlank() } ?: info.applicationInfo.packageName,
                    packageName = info.applicationInfo.packageName,
                    activityName = info.componentName.className,
                    userHandle = info.user
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String, activityName: String = ""): Boolean {
        val user = Process.myUserHandle()
        if (activityName.isNotBlank()) {
            val launched = runCatching {
                launcherApps.startMainActivity(
                    ComponentName(packageName, activityName),
                    user,
                    null,
                    null
                )
                true
            }.getOrDefault(false)
            if (launched) return true
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    fun launchRoleFallback(role: String): Boolean {
        val intent = when (role) {
            "PHONE" -> Intent(Intent.ACTION_DIAL)
            "MESSAGES" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
            "BROWSER" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_BROWSER) }
            "CAMERA" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "CLOCK" -> Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
            "CALENDAR" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }
            else -> null
        } ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun resolveDefaultAppForRole(role: String): InstalledApp? {
        val intent = when (role) {
            "PHONE" -> Intent(Intent.ACTION_DIAL)
            "MESSAGES" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
            "BROWSER" -> Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
            "CAMERA" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            else -> return null
        }
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?: return null
        val pkg = resolved.activityInfo?.packageName ?: return null
        return _installedApps.value.firstOrNull { it.packageName == pkg }
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun requestUninstall(packageName: String) {
        openAppInfo(packageName)
    }

    fun openGoogleDiscover(): Boolean {
        val googlePkg = "com.google.android.googlequicksearchbox"
        val launchIntent = packageManager.getLaunchIntentForPackage(googlePkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(launchIntent); true }.getOrDefault(false)) {
                return true
            }
        }
        val webIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com/".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(webIntent)
            true
        }.getOrDefault(false)
    }

    fun openGoogleSearch(query: String = ""): Boolean {
        val intent = if (query.isNotBlank()) {
            Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_ASSIST).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return runCatching { context.startActivity(intent); true }.getOrDefault(false) ||
            openGoogleDiscover()
    }

    fun openGoogleVoice(): Boolean {
        val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent); true }.getOrDefault(false) ||
            openGoogleDiscover()
    }

    fun unregister() {
        runCatching { launcherApps.unregisterCallback(callback) }
    }
}
