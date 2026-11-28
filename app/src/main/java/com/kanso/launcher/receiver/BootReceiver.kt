package com.kanso.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kanso.launcher.service.KansoLifecycleService
import com.kanso.launcher.service.KansoPerformanceGovernor

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        KansoPerformanceGovernor.start(context.applicationContext)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            KansoLifecycleService.start(context)
        }
    }
}
