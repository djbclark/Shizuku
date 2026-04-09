package moe.shizuku.manager.core.platform.deviceuser

data class DeviceUser(
    val id: Int,
    val name: String,
    val isCurrentUser: Boolean
)