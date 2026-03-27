package moe.shizuku.manager.shizukuservice

import moe.shizuku.manager.shizukuservice.models.StartStep
import moe.shizuku.manager.shizukuservice.models.StepState

class ServiceStartProcess private constructor(
    val steps: List<StartStep>,
    private val runStep: suspend (StartStep) -> Unit
) {
    suspend fun run() {
        steps.forEach { it.reset() }
        for (step in steps) {
            step.state = StepState.Running
            try {
                runStep(step)
                step.state = StepState.Completed
            } catch (e: Throwable) {
                step.state = StepState.Failed(e)
                throw e
            }
        }
    }

    class Builder(private val runStep: suspend (StartStep) -> Unit) {
        private val steps = mutableListOf<StartStep>()

        fun addStep(step: StartStep): Builder {
            steps.add(step)
            return this
        }

        fun build(): ServiceStartProcess {
            return ServiceStartProcess(steps.toList(), runStep)
        }
    }
}
