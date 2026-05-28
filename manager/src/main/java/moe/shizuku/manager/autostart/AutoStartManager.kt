package moe.shizuku.manager.autostart

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import moe.shizuku.manager.autostart.AutoStartWorker.Companion.WORK_NAME
import moe.shizuku.manager.autostart.models.AutoStartState
import moe.shizuku.manager.autostart.notifications.AutoStartNotification
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.PrivilegedServiceStateMachine
import moe.shizuku.manager.start.models.PreStartCheckError

class AutoStartManager(
    private val context: Context,
    private val notificationProvider: AutoStartNotification,
    private val privilegedServiceManager: PrivilegedServiceManager,
    private val privilegedServiceStateMachine: PrivilegedServiceStateMachine
) {

    fun start(forceStart: Boolean = false) {
        if (privilegedServiceStateMachine.isRunning && !forceStart) return

        privilegedServiceManager.checkBackgroundStart()
            .onOk {
                enqueue(privilegedServiceManager.isWifiRequired)
                notificationProvider.updateNotification(
                    AutoStartState.Waiting.FirstRun
                )
            }
            .onErr {
                when (it) {
                    PreStartCheckError.WriteSecureSettingsNotGranted -> {
                        notificationProvider.showPermissionErrorNotification()
                    }

                    else -> Unit // TODO Show generic not supported notification
                }
            }
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        notificationProvider.updateNotification(AutoStartState.Cancelled)
    }

    private fun enqueue(isWifiRequired: Boolean) {
        val workRequestBuilder = OneTimeWorkRequestBuilder<AutoStartWorker>()

        if (isWifiRequired) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

            workRequestBuilder.setConstraints(constraints)
        }

        val workRequest = workRequestBuilder.build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
