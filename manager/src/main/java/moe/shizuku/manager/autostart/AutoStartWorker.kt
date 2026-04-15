package moe.shizuku.manager.autostart

import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.autostart.AutoStartNotificationProvider.Companion.RUNNING_ID
import moe.shizuku.manager.core.utils.runnable.RunnableStatus
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.models.StartStep

class AutoStartWorker(
    context: Context,
    params: WorkerParameters,
    private val notificationProvider: AutoStartNotificationProvider,
    private val privilegedServiceManager: PrivilegedServiceManager,
    private val autoStartManager: AutoStartManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
            val session = privilegedServiceManager.createStartSession()

            coroutineScope {
                val job = launch {
                    session.steps.collect { steps ->
                        checkAwaitingAuth(steps)
                    }
                }

                privilegedServiceManager.startService(session)
                job.cancel()
            }

            val result = session.status.value
            if (result is RunnableStatus.Failed) {
                autoStartManager.reportError(result.throwable)
                return Result.retry()
            } else return Result.success()
    }

    private var isForeground = false

    private suspend fun checkAwaitingAuth(steps: List<StartStep>) {
        val enableWirelessDebuggingStep = steps.find {
            it is StartStep.EnableWirelessDebugging
        } as? StartStep.EnableWirelessDebugging

        val isAwaitingAuth = enableWirelessDebuggingStep?.isAwaitingAuth ?: false

        setProgress(workDataOf(WORK_DATA_AWAITING_AUTH to isAwaitingAuth))

        if (isAwaitingAuth && !isForeground) {
            setForeground(getForegroundInfo())
            isForeground = true
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val fgsType = if (AndroidVersion.isAtLeast14) {
            FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0

        return ForegroundInfo(
            RUNNING_ID,
            notificationProvider.buildNotification(
                applicationContext.getString(R.string.start_background_awaiting_auth)
            ),
            fgsType
        )
    }

    companion object {
        const val WORK_NAME = "adb_start_worker"
        const val WORK_DATA_AWAITING_AUTH = "awaiting_auth"

        fun enqueue(context: Context, isWifiRequired: Boolean) {
            val cb = Constraints.Builder()
            if (isWifiRequired) {
                cb.setRequiredNetworkType(NetworkType.UNMETERED)
            }
            val constraints = cb.build()

            val request =
                OneTimeWorkRequestBuilder<AutoStartWorker>().setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
