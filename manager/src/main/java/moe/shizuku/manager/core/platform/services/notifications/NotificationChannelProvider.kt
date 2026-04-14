package moe.shizuku.manager.core.platform.services.notifications

import androidx.core.app.NotificationChannelCompat

interface NotificationChannelProvider {
    fun provideChannel(): NotificationChannelCompat
}