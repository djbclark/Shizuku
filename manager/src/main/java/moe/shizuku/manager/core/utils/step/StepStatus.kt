package moe.shizuku.manager.core.utils.step

sealed class StepStatus {
    object Pending : StepStatus()
    object Running : StepStatus()
    object Completed : StepStatus()
    data class Failed(val throwable: Throwable) : StepStatus()
}