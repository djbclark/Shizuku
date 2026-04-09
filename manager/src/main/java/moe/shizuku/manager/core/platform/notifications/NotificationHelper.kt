package moe.shizuku.manager.core.platform.notifications

import android.app.NotificationManager
import android.content.Context
import android.os.Build

fun Context.isNotificationChannelEnabled(channelId: String): Boolean {
    val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (!notificationManager.areNotificationsEnabled()) return false

    // Notification channels don't exist on Android 7, so notification is enabled
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true

    // Channel hasn't been created yet, so it will be enabled once created
    val channel = notificationManager.getNotificationChannel(channelId)
        ?: return true

    return channel.importance != NotificationManager.IMPORTANCE_NONE
}