package moe.shizuku.manager.core.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.receiver.ShizukuReceiverStarter

class NotifRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ShizukuReceiverStarter.updateNotification(
            context,
            ShizukuReceiverStarter.WorkerState.RUNNING
        )
    }
}