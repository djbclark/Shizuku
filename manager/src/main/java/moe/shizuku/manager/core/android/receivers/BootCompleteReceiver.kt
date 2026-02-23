package moe.shizuku.manager.core.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.watchdog.services.WatchdogService

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        ShizukuReceiverStarter.start(context)
        if (ShizukuSettings.getWatchdog()) WatchdogService.start(context)
    }
}