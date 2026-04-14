package moe.shizuku.manager.autostart.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.autostart.AutoStartNotificationProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class NotifRestoreReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationProvider: AutoStartNotificationProvider = get()
        // notificationProvider.updateNotification() TODO restore notification
    }
}
