package moe.shizuku.manager.core.utils.step

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class Step(
    private val action: suspend () -> Unit,
    private val mustSucceed: Boolean = true
) {
    private val _status = MutableStateFlow<StepStatus>(StepStatus.Pending)
    val status: StateFlow<StepStatus> = _status

    suspend fun run() {
        _status.value = StepStatus.Running
        try {
            action()
            _status.value = StepStatus.Completed
        } catch (t: Throwable) {
            _status.value = StepStatus.Failed(t)
            if (mustSucceed) throw t
        }
    }
}