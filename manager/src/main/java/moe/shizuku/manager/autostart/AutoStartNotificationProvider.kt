package moe.shizuku.manager.autostart

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import moe.shizuku.manager.R
import moe.shizuku.manager.autostart.models.AutoStartState
import moe.shizuku.manager.autostart.receivers.NotifAttemptReceiver
import moe.shizuku.manager.autostart.receivers.NotifCancelReceiver
import moe.shizuku.manager.autostart.receivers.NotifRestoreReceiver
import moe.shizuku.manager.core.platform.services.notifications.NotificationChannelProvider
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper

class AutoStartNotificationProvider(
    private val context: Context,
    private val notificationHelper: NotificationHelper
): NotificationChannelProvider {
    companion object {
        const val CHANNEL_ID = "autostart"
        const val RUNNING_ID: Int = 1447
        const val ENQUEUED_ID: Int = 1448
        const val RESULT_ID: Int = 1449
    }

    override fun provideChannel(): NotificationChannelCompat =
        NotificationChannelCompat.Builder(
            "autostart",
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(context.getString(R.string.start_background))
            .build()

    fun buildNotification(msg: String? = null): Notification {
        val cancelIntent = Intent(context, NotifCancelReceiver::class.java)
        val cancelPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val attemptNowIntent = Intent(context, NotifAttemptReceiver::class.java)
        val attemptNowPendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                attemptNowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val restoreIntent = Intent(context, NotifRestoreReceiver::class.java)
        val restorePendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                restoreIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val nb = NotificationCompat.Builder(context, CHANNEL_ID)

        if (msg != null) nb.setContentText(msg)

        return nb
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(context.getString(R.string.start_background))
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                R.drawable.ic_server_restart,
                context.getString(R.string.start_background_attempt_now),
                attemptNowPendingIntent
            )
            .addAction(
                R.drawable.ic_close_24,
                context.getString(android.R.string.cancel),
                cancelPendingIntent
            )
            .setDeleteIntent(restorePendingIntent)
            .build()
    }

    fun updateNotification(state: AutoStartState) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val msgId = when (state) {
            is AutoStartState.Running -> {
                if (state.isAwaitingAuth) R.string.start_background_awaiting_auth
                else null
            }
            is AutoStartState.Waiting -> when (state.reason) {
                AutoStartState.Waiting.Reason.WIFI -> R.string.start_background_awaiting_wifi
                AutoStartState.Waiting.Reason.RETRY -> R.string.start_background_awaiting_retry
                AutoStartState.Waiting.Reason.FIRST_RUN -> null
            }
            is AutoStartState.Success, is AutoStartState.Cancelled -> {
                nm.cancel(RUNNING_ID)
                nm.cancel(ENQUEUED_ID)
                return
            }
        }
        val msg = if (msgId != null) context.getString(msgId) else null

        val notificationId = if (state is AutoStartState.Running) {
            nm.cancel(ENQUEUED_ID)
            RUNNING_ID
        } else {
            nm.cancel(RUNNING_ID)
            ENQUEUED_ID
        }

        nm.notify(notificationId, buildNotification(msg))
    }

    fun showErrorNotification(t: Throwable) {
        val msg = t.message ?: context.getString(R.string.error)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(context.getString(R.string.start_background_error))
            .setContentText(msg)
            .setSilent(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .build()

        notificationHelper.notify(RESULT_ID, notification)
    }

    fun showPermissionErrorNotification() {
        val webpageIntent =
            Intent(
                Intent.ACTION_VIEW,
                "https://github.com/thedjchi/Shizuku/wiki#shizuku-isnt-starting-on-boot-for-me".toUri()
            )
        val pendingWebpageIntent =
            PendingIntent.getActivity(
                context,
                0,
                webpageIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val msg = context.getString(R.string.start_background_error_permission_message)

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_system_icon)
                .setContentTitle(context.getString(R.string.start_background_error_permission))
                .setContentText(msg)
                .setSilent(true)
                .setContentIntent(pendingWebpageIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .build()

        notificationHelper.notify(RESULT_ID, notification)
    }
}