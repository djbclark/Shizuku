package moe.shizuku.manager.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.receivers.NotifAttemptReceiver
import moe.shizuku.manager.core.android.receivers.NotifCancelReceiver
import moe.shizuku.manager.core.android.receivers.NotifRestoreReceiver
import moe.shizuku.manager.core.android.settings.SystemSettingsPage
import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.shizukuservice.workers.AdbStartWorker
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.core.utils.UserHandleCompat
import moe.shizuku.manager.core.data.preferences.PreferencesRepository

class ShizukuReceiverStarter(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val environmentUtils: EnvironmentUtils,
    private val shizukuStateMachine: ShizukuStateMachine,
    private val starter: Starter,
    private val userHandleCompat: UserHandleCompat
) {
    companion object {
        const val NOTIFICATION_ID = 1447
        private const val CHANNEL_ID = "AdbStartWorker"
    }

    enum class WorkerState {
        AWAITING_WIFI,
        AWAITING_RETRY,
        RUNNING,
        STOPPED,
    }

    fun start(forceStart: Boolean = false) {
        if ((userHandleCompat.myUserId() > 0 || shizukuStateMachine.isRunning()) && !forceStart) return

        if (preferencesRepository.startMode.get() == StartMode.ROOT) {
            rootStart()
        } else if ((
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || environmentUtils.isTelevision() ||
                            environmentUtils.getAdbTcpPort() > 0
                    ) &&
            preferencesRepository.startMode.get() == StartMode.WADB
        ) {
            if (context.hasWriteSecureSettings()) {
                AdbStartWorker.enqueue(context)
                updateNotification(WorkerState.AWAITING_WIFI)
            } else {
                showPermissionErrorNotification()
            }
        } else {
            Log.w(TAG, "Background start not supported")
        }
    }

    fun buildNotification(msg: String? = null): Notification {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.start_background),
                NotificationManager.IMPORTANCE_LOW,
            )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

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

        val wifiIntent = SystemSettingsPage.InternetPanel.buildIntent(context)
        val wifiPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                wifiIntent,
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
            .setContentIntent(wifiPendingIntent)
            .build()
    }

    fun updateNotification(state: WorkerState) {
        if (state == WorkerState.STOPPED) return
        val msgId =
            when (state) {
                WorkerState.AWAITING_WIFI -> R.string.start_background_awaiting_wifi
                WorkerState.AWAITING_RETRY -> R.string.start_background_awaiting_retry
                else -> null
            }
        val msg = if (msgId != null) context.getString(msgId) else null
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(msg))
    }

    private fun rootStart() {
        if (!Shell.getShell().isRoot) {
            Shell.getCachedShell()?.close()
            return
        }

        try {
            shizukuStateMachine.set(ShizukuStateMachine.State.STARTING)
            Shell.cmd(starter.internalCommand).exec()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Shizuku with root", e)
            shizukuStateMachine.update()
        }
    }

    private fun showPermissionErrorNotification() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.start_background),
                NotificationManager.IMPORTANCE_LOW,
            )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

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

        nm.notify(NOTIFICATION_ID, notification)
    }
}
