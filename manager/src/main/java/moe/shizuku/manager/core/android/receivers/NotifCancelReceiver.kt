package moe.shizuku.manager.core.android.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import moe.shizuku.manager.receiver.ShizukuReceiverStarter

class NotifCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkManager.getInstance(context).cancelUniqueWork("adb_start_worker")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ShizukuReceiverStarter.NOTIFICATION_ID)
    }
}
