package moe.shizuku.manager.watchdog.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.core.platform.settings.SettingsIntentFactory

class CrashNotification(
    private val context: Context,
    private val channel: AppNotificationChannel,
    private val notificationHelper: NotificationHelper,
    private val settingsIntentFactory: SettingsIntentFactory
) {

    fun showCrashNotification() {
        val learnMoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://github.com/thedjchi/Shizuku/wiki/troubleshooting#shizuku-keeps-stopping-randomly".toUri()
        }
        val learnMorePendingIntent = PendingIntent.getActivity(
            context, 0, learnMoreIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val nb = NotificationCompat.Builder(context, channel.id)
            .setContentTitle(context.getString(R.string.watchdog_crash_alert))
            .setContentText(context.getString(R.string.watchdog_crash_alert_message))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(learnMorePendingIntent)
            .setAutoCancel(true)

        if (AndroidVersion.isAtLeast8) {
            val disableIntent = settingsIntentFactory.notifications(channel.id)
            val disablePendingIntent = PendingIntent.getActivity(
                context, 0, disableIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            nb.addAction(0, context.getString(R.string.watchdog_disable_alerts), disablePendingIntent)
        }

        val notification = nb.build()
        notificationHelper.notify(ID_CRASH, notification)
    }

    companion object {
        const val ID_CRASH: Int = 1002
    }

}
