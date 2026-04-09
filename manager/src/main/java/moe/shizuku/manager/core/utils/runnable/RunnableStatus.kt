package moe.shizuku.manager.core.utils.runnable

sealed class RunnableStatus {
    object Pending : RunnableStatus()
    object Running : RunnableStatus()
    object Completed : RunnableStatus()
    data class Failed(val throwable: Throwable) : RunnableStatus()

    val isFinished: Boolean = this is Completed || this is Failed
}