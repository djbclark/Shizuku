package moe.shizuku.manager.autostart.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.PendingIntentCompat
import moe.shizuku.manager.R
import moe.shizuku.manager.autostart.models.AutoStartState
import moe.shizuku.manager.autostart.receivers.NotifAttemptReceiver
import moe.shizuku.manager.autostart.receivers.NotifCancelReceiver
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper

class AutoStartNotification(
    private val context: Context,
    private val channel: AppNotificationChannel,
    private val notificationHelper: NotificationHelper
) {

    fun buildNotification(state: AutoStartState): Notification {
        val msgId = when (state) {
            is AutoStartState.Running -> {
                if (state.isAwaitingAuth) R.string.start_background_awaiting_auth
                else null
            }

            is AutoStartState.Waiting.Wifi -> R.string.start_background_awaiting_wifi
            is AutoStartState.Waiting.Retry -> R.string.start_background_awaiting_retry

            else -> null
        }

        val cancelIntent = Intent(context, NotifCancelReceiver::class.java)
        val cancelPendingIntent = cancelIntent.toPendingIntent()

        val attemptNowIntent = Intent(context, NotifAttemptReceiver::class.java)
        val attemptNowPendingIntent = attemptNowIntent.toPendingIntent()

        val nb = NotificationCompat.Builder(context, channel.id)

        msgId?.let {
            val msg = context.getString(it)
            nb.setContentText(msg)
        }

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
            .build()
    }

    private fun Intent.toPendingIntent() =
        PendingIntentCompat.getBroadcast(
            context,
            0,
            this,
            PendingIntent.FLAG_UPDATE_CURRENT,
            true
        )

    fun updateNotification(state: AutoStartState) {
        if (state.isFinished) {
            notificationHelper.cancel(ENQUEUED_ID)
            notificationHelper.cancel(RUNNING_ID)
            return
        }

        var notificationId: Int
        if (state is AutoStartState.Running) {
            notificationHelper.cancel(ENQUEUED_ID)
            notificationId = RUNNING_ID
        } else {
            notificationHelper.cancel(RUNNING_ID)
            notificationId = ENQUEUED_ID
        }

        val notification = buildNotification(state)
        notificationHelper.notify(notificationId, notification)
    }

    fun showErrorNotification() {
        val msg = TODO()
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(context.getString(R.string.start_background_error))
            .setContentText(msg)
            .setSilent(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .build()

        notificationHelper.notify(RESULT_ID, notification)
    }

    fun showPermissionErrorNotification() {
        val notification =
            NotificationCompat
                .Builder(context, channel.id)
                .setSmallIcon(R.drawable.ic_system_icon)
                .setContentTitle(context.getString(R.string.start_background_error_permission))
                .setContentText(context.getString(R.string.start_background_error_permission_message))
                .build()

        notificationHelper.notify(RESULT_ID, notification)
    }

    companion object {
        const val RUNNING_ID: Int = 1447
        const val ENQUEUED_ID: Int = 1448
        const val RESULT_ID: Int = 1449
    }

}