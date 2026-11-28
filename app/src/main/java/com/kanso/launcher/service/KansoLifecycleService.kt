package com.kanso.launcher.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kanso.launcher.KansoApplication
import com.kanso.launcher.LauncherActivity
import com.kanso.launcher.R
import com.kanso.launcher.ui.lockscreen.LockscreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Background Lifecycle & 60FPS Governor Service:
 * - Hosts KansoPerformanceGovernor to dynamically limit background CPU during animations.
 * - Monitors ACTION_SCREEN_ON ONLY when the user has explicitly enabled the Optional Lockscreen
 *   Companion (off by default to respect manufacturer lockscreen protection).
 */
class KansoLifecycleService : Service() {

    companion object {
        private const val CHANNEL_ID = "kanso_governor_channel"
        private const val NOTIFICATION_ID = 4108

        fun start(context: Context) {
            val intent = Intent(context, KansoLifecycleService::class.java)
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.stopService(Intent(context, KansoLifecycleService::class.java))
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Volatile
    private var lockscreenCompanionActive = false
    private var screenReceiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    KansoAccessibilityService.resetLockCycleLatch()
                    if (lockscreenCompanionActive) {
                        val lockIntent = Intent(this@KansoLifecycleService, LockscreenActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            )
                        }
                        runCatching { startActivity(lockIntent) }
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    if (!lockscreenCompanionActive) return
                    val lockIntent = Intent(this@KansoLifecycleService, LockscreenActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                    }
                    runCatching { startActivity(lockIntent) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        val notif = buildNotification()
        val fgStarted = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
            true
        }.getOrDefault(false)

        if (!fgStarted) {
            stopSelf()
            return
        }

        val app = application as? KansoApplication
        if (app != null) {
            serviceScope.launch {
                app.preferences.settingsFlow.collectLatest { settings ->
                    lockscreenCompanionActive = settings.lockscreenCompanionEnabled
                    KansoPerformanceGovernor.setEnabled(this@KansoLifecycleService, settings.performanceGovernorEnabled)
                    updateScreenReceiverRegistration(settings.lockscreenCompanionEnabled)
                }
            }
        } else {
            KansoPerformanceGovernor.start(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        updateScreenReceiverRegistration(false)
        KansoPerformanceGovernor.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateScreenReceiverRegistration(shouldRegister: Boolean) {
        if (shouldRegister && !screenReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            runCatching {
                ContextCompat.registerReceiver(
                    this,
                    screenReceiver,
                    filter,
                    ContextCompat.RECEIVER_EXPORTED
                )
                screenReceiverRegistered = true
            }
        } else if (!shouldRegister && screenReceiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
    }

    private fun ensureNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.lifecycle_service_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_kanso_status)
            .setContentTitle(getString(R.string.lifecycle_service_notif_title))
            .setContentText(getString(R.string.lifecycle_service_notif_text))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pi)
            .build()
    }
}
