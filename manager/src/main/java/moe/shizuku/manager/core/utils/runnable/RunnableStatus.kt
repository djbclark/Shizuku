package moe.shizuku.manager.core.utils.runnable

sealed class RunnableStatus {
    object Pending : RunnableStatus()
    object Running : RunnableStatus()
    object Completed : RunnableStatus()
    data class Failed(val throwable: Throwable) : RunnableStatus()

    val isFinished = this is Completed || this is Failed
}