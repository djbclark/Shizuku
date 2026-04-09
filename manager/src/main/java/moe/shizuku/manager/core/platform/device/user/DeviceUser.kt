package moe.shizuku.manager.core.platform.device.user

data class DeviceUser(
    val id: Int,
    val name: String,
    val isCurrentUser: Boolean
)