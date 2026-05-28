package moe.shizuku.manager.core.platform.services.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

class NotificationChannelManager(
    private val context: Context,
    private val appNotificationChannels: List<AppNotificationChannel>
) {
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    fun createChannels() {
        val channels = appNotificationChannels.map { createChannel(it) }
        notificationManager.createNotificationChannelsCompat(channels)
    }

    fun createChannel(channel: AppNotificationChannel): NotificationChannelCompat =
        NotificationChannelCompat.Builder(channel.id, channel.importance)
            .apply {
                setName(context.getString(channel.name))
                channel.customizer.invoke(this) // Apply custom builder functions defined in channel
            }
            .build()

}