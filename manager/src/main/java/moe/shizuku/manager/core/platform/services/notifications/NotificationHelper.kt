package moe.shizuku.manager.core.platform.services.notifications

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import moe.shizuku.manager.core.extensions.hasPermission
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.systemService

class NotificationHelper(
    private val context: Context,
    private val notificationChannels: List<NotificationChannelProvider>
) {
    private val notificationManager: NotificationManagerCompat by systemService(context)

    private val hasPostNotificationsPermission: Boolean
        get() = if (AndroidVersion.isAtLeast13) {
            context.hasPermission(POST_NOTIFICATIONS)
        } else true

    fun createChannels() {
        val channels = notificationChannels.map { it.provideChannel() }
        notificationManager.createNotificationChannelsCompat(channels)
    }

    fun notify(id: Int, notification: Notification) {
        if (hasPostNotificationsPermission) {
            @SuppressLint("MissingPermission")
            notificationManager.notify(id, notification)
        }
    }

    fun isNotificationChannelEnabled(channelId: String): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false

        // Notification channels don't below Android 8, so notification is enabled
        if (!AndroidVersion.isAtLeast8) return true

        // Channel hasn't been created yet, so it will be enabled once created
        val channel = notificationManager.getNotificationChannelCompat(channelId)
            ?: return true

        val channelGroup = channel.group?.let {
            notificationManager.getNotificationChannelGroupCompat(it)
        } ?: return true

        return channel.importance != NotificationManager.IMPORTANCE_NONE &&
                !channelGroup.isBlocked

    }
}