package moe.shizuku.manager.core.utils.runnable

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import moe.shizuku.manager.core.extensions.TAG

abstract class Runnable(
    private val action: suspend () -> Unit,
    private val throws: Boolean = true
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
            Log.e(TAG, "Failed to run action", t)
            if (throws) throw t
        }
    }
}