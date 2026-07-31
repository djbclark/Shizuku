package moe.shizuku.manager.receiver

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import moe.shizuku.manager.utils.HeadlessLogger
import moe.shizuku.manager.utils.ShizukuStateMachine

class BootRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val VERIFY_DELAY_MS = 3000L
    }

    // No attempt cap: a device blocked on something only a human can do at boot (unlock for FBE,
    // authorize a new network, pair over ADB) may not get that human's attention for hours or
    // days. Giving up after a handful of quick retries (the previous 5-attempt cap exhausted
    // within ~5 minutes of boot, via WorkManager's own exponential backoff) meant Shizuku would
    // never try again until the next full reboot, no matter how long the wait — confirmed live
    // against issue #43's CLOSED_NO_SHELL soak on hd8, where Shizuku itself doesn't even start
    // until the device is physically unlocked. WorkManager's own backoff still applies here
    // (EXPONENTIAL from BootCompleteReceiver's 10s base, capped at its ~5h internal maximum), so
    // this keeps trying indefinitely at a reasonable cadence rather than hammering the device.
    override suspend fun doWork(): Result {
        if (ShizukuStateMachine.isRunning()) {
            HeadlessLogger.i("BootRetry", "Shizuku already running (attempt $runAttemptCount)")
            return Result.success()
        }

        HeadlessLogger.i("BootRetry", "Retrying Shizuku start (attempt $runAttemptCount)")
        ShizukuReceiverStarter.start(applicationContext)

        delay(VERIFY_DELAY_MS)
        if (ShizukuStateMachine.isRunning()) {
            HeadlessLogger.i("BootRetry", "Start succeeded (attempt $runAttemptCount)")
            return Result.success()
        }

        HeadlessLogger.w("BootRetry", "Start not yet running, will retry (attempt $runAttemptCount)")
        return Result.retry()
    }
}
