package moe.shizuku.manager.autostart

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.shizuku.manager.autostart.models.toAutoStartState
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.platform.services.user.DeviceUserRepository
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.models.PreStartCheck
import moe.shizuku.manager.privilegedservice.data.ShizukuStateMachine
import java.io.EOFException
import java.util.concurrent.TimeoutException

class AutoStartManager(
    private val context: Context,
    private val notificationProvider: AutoStartNotificationProvider,
    private val shizukuStateMachine: ShizukuStateMachine,
    private val privilegedServiceManager: PrivilegedServiceManager,
    private val deviceUserRepository: DeviceUserRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        observeWorker()
    }

    private fun observeWorker() {
        val workManager = WorkManager.getInstance(context)
        scope.launch {workManager.getWorkInfosForUniqueWorkFlow(AutoStartWorker.WORK_NAME)
            .map { it.firstOrNull()?.toAutoStartState() }
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest { state ->
                notificationProvider.updateNotification(state)
            }
        }
    }

    fun start(forceStart: Boolean = false) {
        if ((deviceUserRepository.getCurrentUser().id > 0 || shizukuStateMachine.isRunning()) && !forceStart) return

        when (privilegedServiceManager.canStartInBackground()) {
            PreStartCheck.Success -> {
                AutoStartWorker.enqueue(context, privilegedServiceManager.isWifiRequired)
            }

            PreStartCheck.Failure.WriteSecureSettingsNotGranted -> {
                notificationProvider.showPermissionErrorNotification()
            }

            else -> Log.w(TAG, "Background start not supported")
        }
    }

    fun reportError(t: Throwable) {
        val ignored = listOf(
            EOFException::class,
            SecurityException::class,
            TimeoutException::class,
        )
        if (ignored.none { it.isInstance(t) }) {
            notificationProvider.showErrorNotification(t)
        }
    }
}
