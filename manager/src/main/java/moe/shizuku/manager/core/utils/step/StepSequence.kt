package moe.shizuku.manager.core.utils.step

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class StepSequence<T : Step>(
    private val steps: List<T>,
    coroutineScope: CoroutineScope
) {
    val stepFlow: StateFlow<List<T>> =
        combine(steps.map { it.status }) { steps }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                initialValue = steps
            )

    suspend fun run() {
        for (step in steps) step.run()
    }
}