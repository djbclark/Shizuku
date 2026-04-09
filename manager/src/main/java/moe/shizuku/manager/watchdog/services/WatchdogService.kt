package moe.shizuku.manager.watchdog.services

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.shizuku.manager.autostart.AutoStartManager
import moe.shizuku.manager.privilegedservice.data.ShizukuStateMachine
import moe.shizuku.manager.watchdog.utils.WatchdogNotifications
import org.koin.android.ext.android.inject

class WatchdogService : Service() {

    private val watchdogNotifications: WatchdogNotifications by inject()
    private val autoStartManager: AutoStartManager by inject()
    private val shizukuStateMachine: ShizukuStateMachine by inject()

    private val crashListener: (ShizukuStateMachine.State) -> Unit = {
        if (it == ShizukuStateMachine.State.CRASHED) {
            watchdogNotifications.showCrashNotification()
            autoStartManager.start()
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        shizukuStateMachine.addListener(crashListener)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0

        ServiceCompat.startForeground(
            this,
            WatchdogNotifications.ID_WATCHDOG,
            watchdogNotifications.createWatchdogNotification(),
            fgsType,
        )
        return START_STICKY
    }

    override fun onDestroy() {
        shizukuStateMachine.removeListener(crashListener)
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}
