package moe.shizuku.manager.core.utils.runnable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException

abstract class Runnable(
    private val throws: Boolean = true
) {
    private val _status = MutableStateFlow<RunnableStatus>(RunnableStatus.Pending)
    val status: StateFlow<RunnableStatus> = _status

    // Emits a new running status
    // Useful when a runnable implementation provides additional status information
    fun refresh() {
        _status.update { current ->
            if (current is RunnableStatus.Running) {
                RunnableStatus.Running()
            } else {
                current
            }
        }
    }

    suspend fun run() {
        _status.update { RunnableStatus.Running() }
        runCatching { onRun() }
            .onSuccess { _status.update { RunnableStatus.Completed } }
            .onFailure { e ->
                _status.update { RunnableStatus.Failed(e) }
                if (throws || e is CancellationException || e !is Exception) throw e
            }
    }

    abstract suspend fun onRun()
}