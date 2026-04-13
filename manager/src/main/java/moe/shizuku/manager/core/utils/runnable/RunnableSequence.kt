package moe.shizuku.manager.core.utils.runnable

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import moe.shizuku.manager.core.extensions.TAG

class RunnableSequence<T : Runnable>(
    private val _steps: List<T>,
    throws: Boolean = false,
) : Runnable(
    action = {
        _steps.forEach {
            Log.i(TAG, "Running step: ${it::class.simpleName}")
            it.run()
        }
             },
    throws = throws,
) {
    val steps: Flow<List<T>> =
        combine(_steps.map { it.status }) { _steps }
}
