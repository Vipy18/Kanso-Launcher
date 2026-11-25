package com.kanso.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanso.launcher.service.KansoLifecycleService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        KansoLifecycleService.start(context)
    }
}
