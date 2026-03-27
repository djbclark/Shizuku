package moe.shizuku.manager.core.utils.runnable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class Runnable(
    private val action: suspend () -> Unit,
    private val mustSucceed: Boolean = true
) {
    private val _status = MutableStateFlow<RunnableStatus>(RunnableStatus.Pending)
    val status: StateFlow<RunnableStatus> = _status

    suspend fun run() {
        _status.value = RunnableStatus.Running
        try {
            action()
            _status.value = RunnableStatus.Completed
        } catch (t: Throwable) {
            _status.value = RunnableStatus.Failed(t)
            if (mustSucceed) throw t
        }
    }
}