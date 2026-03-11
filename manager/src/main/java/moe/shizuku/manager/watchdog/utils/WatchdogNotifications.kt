package moe.shizuku.manager.watchdog.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.settings.SystemSettingsPage
import moe.shizuku.manager.core.ui.MainActivity
import moe.shizuku.manager.watchdog.services.WatchdogService

object WatchdogNotifications {

    const val ID_WATCHDOG = 1001
    const val ID_CRASH = 1002
    const val CHANNEL_ID_WATCHDOG = "shizuku_watchdog"
    const val CHANNEL_ID_CRASH = "crash_reports"

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val watchdogChannel = NotificationChannel(
            CHANNEL_ID_WATCHDOG,
            context.getString(R.string.settings_watchdog),
            NotificationManager.IMPORTANCE_LOW,
        )

        val crashChannel = NotificationChannel(
            CHANNEL_ID_CRASH,
            context.getString(R.string.watchdog_crash_reports),
            NotificationManager.IMPORTANCE_DEFAULT,
        )

        manager.createNotificationChannels(listOf(watchdogChannel, crashChannel))
    }

    fun createWatchdogNotification(context: Context): Notification {
        createChannels(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(context, WatchdogService::class.java).apply {
            action = WatchdogService.ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_WATCHDOG)
            .setContentTitle(context.getString(R.string.watchdog_running))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(launchPendingIntent)
            .addAction(
                R.drawable.ic_close_24,
                context.getString(R.string.disable),
                stopPendingIntent,
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun showCrashNotification(context: Context) {
        createChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val learnMoreIntent = Intent(Intent.ACTION_VIEW).apply {
            data =
                "https://github.com/thedjchi/Shizuku/wiki/troubleshooting#shizuku-keeps-stopping-randomly".toUri()
        }
        val learnMorePendingIntent = PendingIntent.getActivity(
            context, 0, learnMoreIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val disableIntent =
            SystemSettingsPage.Notifications.NotificationChannel.buildIntent(context)
        val disablePendingIntent = PendingIntent.getActivity(
            context, 0, disableIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CRASH)
            .setContentTitle(context.getString(R.string.watchdog_crash_alert))
            .setContentText(context.getString(R.string.watchdog_crash_alert_message))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(learnMorePendingIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.watchdog_disable_alerts), disablePendingIntent)
            .build()

        nm.notify(ID_CRASH, notification)
    }
}