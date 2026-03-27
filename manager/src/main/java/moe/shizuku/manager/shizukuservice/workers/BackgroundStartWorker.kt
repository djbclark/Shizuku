package moe.shizuku.manager.shizukuservice.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import moe.shizuku.manager.R
import moe.shizuku.manager.shizukuservice.ShizukuReceiverStarter
import moe.shizuku.manager.shizukuservice.ShizukuServiceManager
import moe.shizuku.manager.utils.ShizukuStateMachine
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.EOFException
import java.util.concurrent.TimeoutException

class BackgroundStartWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val shizukuReceiverStarter: ShizukuReceiverStarter = get()
    private val shizukuStateMachine: ShizukuStateMachine = get()
    private val shizukuServiceManager: ShizukuServiceManager = get()

    override suspend fun doWork(): Result {
        try {
            shizukuReceiverStarter.updateNotification(
                ShizukuReceiverStarter.WorkerState.RUNNING,
            )

            shizukuServiceManager.startService(
                onShowForeground = { notification ->
                    val foregroundInfo = ForegroundInfo(
                        ShizukuReceiverStarter.NOTIFICATION_ID,
                        notification,
                    )
                    setForeground(foregroundInfo)
                },
                buildForegroundNotification = {
                    shizukuReceiverStarter.buildNotification(null)
                }
            )

            val nm =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(ShizukuReceiverStarter.NOTIFICATION_ID)

            return Result.success()
        } catch (e: CancellationException) {
            val state =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    ShizukuReceiverStarter.WorkerState.AWAITING_RETRY
                } else {
                    when (stopReason) {
                        WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY -> ShizukuReceiverStarter.WorkerState.AWAITING_WIFI
                        WorkInfo.STOP_REASON_CANCELLED_BY_APP -> ShizukuReceiverStarter.WorkerState.STOPPED
                        else -> ShizukuReceiverStarter.WorkerState.AWAITING_RETRY
                    }
                }
            shizukuReceiverStarter.updateNotification(state)

            throw e
        } catch (e: Exception) {
            val ignored =
                listOf(
                    EOFException::class,
                    SecurityException::class,
                    TimeoutException::class,
                )
            if (ignored.none { it.isInstance(e) }) showErrorNotification(applicationContext, e)

            if (shizukuStateMachine.update() == ShizukuStateMachine.State.RUNNING) {
                return Result.success()
            } else {
                shizukuReceiverStarter.updateNotification(
                    ShizukuReceiverStarter.WorkerState.AWAITING_RETRY,
                )
                return Result.retry()
            }
        }
    }

    private fun showErrorNotification(
        context: Context,
        e: Exception,
    ) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.start_background),
                NotificationManager.IMPORTANCE_LOW,
            )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val nb = NotificationCompat.Builder(context, CHANNEL_ID)

        val notification =
            nb
                .setSmallIcon(R.drawable.ic_system_icon)
                .setContentTitle(context.getString(R.string.start_background_error))
                .setContentText(e.message)
                .setSilent(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(e.message))
                .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        fun enqueue(context: Context, isWifiRequired: Boolean) {
            val cb = Constraints.Builder()
            if (isWifiRequired) {
                cb.setRequiredNetworkType(NetworkType.UNMETERED)
            }
            val constraints = cb.build()

            val request =
                OneTimeWorkRequestBuilder<BackgroundStartWorker>()
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "adb_start_worker",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        const val CHANNEL_ID = "AdbStartWorker"
        const val NOTIFICATION_ID = 1448
    }
}
