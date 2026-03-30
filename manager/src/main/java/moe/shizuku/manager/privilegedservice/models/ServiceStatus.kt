package moe.shizuku.manager.privilegedservice.models

data class ServiceStatus(
    val uid: Int = -1,
    val apiVersion: Int = -1,
    val patchVersion: Int = -1,
    val seContext: String? = null,
    val permission: Boolean = false,
    val isRunning: Boolean = false
)
