package moe.shizuku.manager.core.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.shizukuservice.workers.BackgroundStartWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NotifAttemptReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val environmentUtils: EnvironmentUtils = get()
        BackgroundStartWorker.enqueue(context, environmentUtils.isWifiRequired())
    }
}
