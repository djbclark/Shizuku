package moe.shizuku.manager.core.utils.runnable

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RunnableSequence<T : Runnable>(
    private val _steps: List<T>
) : Runnable(action = { _steps.forEach { it.run() } }) {

    val steps: Flow<List<T>> =
        combine(_steps.map { it.status }) { _steps }
}
