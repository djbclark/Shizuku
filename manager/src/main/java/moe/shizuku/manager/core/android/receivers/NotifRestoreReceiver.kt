package moe.shizuku.manager.core.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NotifRestoreReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val shizukuReceiverStarter: ShizukuReceiverStarter = get()
        shizukuReceiverStarter.updateNotification(
            ShizukuReceiverStarter.WorkerState.RUNNING
        )
    }
}
