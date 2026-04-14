package moe.shizuku.manager.autostart.models

import androidx.work.WorkInfo
import moe.shizuku.manager.autostart.AutoStartWorker.Companion.WORK_DATA_AWAITING_AUTH
import moe.shizuku.manager.core.platform.device.AndroidVersion

sealed class AutoStartState {
    data class Waiting(val reason: Reason) : AutoStartState() {
        enum class Reason { WIFI, RETRY, FIRST_RUN }
    }
    data class Running(val isAwaitingAuth: Boolean) : AutoStartState()
    object Success : AutoStartState()
    object Cancelled : AutoStartState()
}

fun WorkInfo.toAutoStartState(): AutoStartState? {
    return when (state) {
        WorkInfo.State.ENQUEUED -> {
            val isWifiLost = AndroidVersion.isAtLeast12 &&
                    stopReason == WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY

            if (isWifiLost) {
                AutoStartState.Waiting(AutoStartState.Waiting.Reason.WIFI)
            } else if (runAttemptCount > 0) {
                AutoStartState.Waiting(AutoStartState.Waiting.Reason.RETRY)
            } else {
                AutoStartState.Waiting(AutoStartState.Waiting.Reason.FIRST_RUN)
            }
        }
        WorkInfo.State.RUNNING -> {
            val isAwaitingAuth = progress.getBoolean(WORK_DATA_AWAITING_AUTH, false)
            AutoStartState.Running(isAwaitingAuth)
        }
        WorkInfo.State.SUCCEEDED -> AutoStartState.Success
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> AutoStartState.Cancelled
        else -> null
    }
}