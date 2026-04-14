package moe.shizuku.manager.autostart.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import moe.shizuku.manager.autostart.AutoStartManager
import org.koin.core.component.KoinComponent

class NotifCancelReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).cancelUniqueWork("adb_start_worker")
    }
}
