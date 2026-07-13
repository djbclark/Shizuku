package moe.shizuku.manager.receiver

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import moe.shizuku.manager.utils.HeadlessLogger
import moe.shizuku.manager.utils.ShizukuStateMachine

class BootRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (ShizukuStateMachine.isRunning()) {
            HeadlessLogger.i("BootRetry", "Shizuku already running, no retry needed")
            return Result.success()
        }

        HeadlessLogger.i("BootRetry", "Retrying Shizuku start after boot")
        ShizukuReceiverStarter.start(applicationContext)

        return Result.success()
    }
}
