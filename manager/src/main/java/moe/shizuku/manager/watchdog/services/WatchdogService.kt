package moe.shizuku.manager.watchdog.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.settings.SystemSettingsPage
import moe.shizuku.manager.core.ui.MainActivity
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.utils.ShizukuStateMachine

private const val NOTIFICATION_ID_WATCHDOG = 1001
private const val NOTIFICATION_ID_CRASH = 1002

class WatchdogService : Service() {
    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (it == ShizukuStateMachine.State.CRASHED) {
            showCrashNotification()
            ShizukuReceiverStarter.start(applicationContext)
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == "ACTION_STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID_WATCHDOG,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(
                NOTIFICATION_ID_WATCHDOG,
                buildNotification(),
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ShizukuStateMachine.removeListener(stateListener)
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "shizuku_watchdog"
        val channelName = "Watchdog"

        val channel =
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW,
            )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val launchIntent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
        val launchPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent =
            Intent(this, WatchdogService::class.java).apply {
                action = "ACTION_STOP_SERVICE"
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, channelId)
            .setContentTitle(getString(R.string.watchdog_running))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(launchPendingIntent)
            .addAction(
                R.drawable.ic_close_24,
                getString(R.string.disable),
                stopPendingIntent,
            ).setOngoing(true)
            .build()
    }

    private fun showCrashNotification() {
        val channelId = CRASH_CHANNEL_ID
        val channelName = "Crash Reports"

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        nm.createNotificationChannel(channel)

        val learnMoreIntent =
            Intent(Intent.ACTION_VIEW).apply {
                data =
                    "https://github.com/thedjchi/Shizuku/wiki#shizuku-keeps-stopping-randomly".toUri()
            }
        val learnMorePendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                learnMoreIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val disableIntent =
            SystemSettingsPage.Notifications.NotificationChannel.buildIntent(applicationContext)
        val disablePendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                disableIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(this, channelId)
                .setContentTitle(getString(R.string.watchdog_crash_alert))
                .setContentText(getString(R.string.watchdog_crash_alert_message))
                .setSmallIcon(R.drawable.ic_system_icon)
                .setContentIntent(learnMorePendingIntent)
                .setAutoCancel(true)
                .addAction(0, getString(R.string.watchdog_disable_alerts), disablePendingIntent)
                .build()

        nm.notify(NOTIFICATION_ID_CRASH, notification)
    }

    companion object {
        const val CRASH_CHANNEL_ID = "crash_reports"
        private val _isRunning = MutableStateFlow(false)

        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}