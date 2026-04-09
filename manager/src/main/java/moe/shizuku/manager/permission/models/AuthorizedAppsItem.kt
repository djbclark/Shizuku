package moe.shizuku.manager.permission.models

import android.content.pm.ApplicationInfo
import moe.shizuku.manager.core.platform.device.user.DeviceUser

sealed class AuthorizedAppsItem(val viewType: ViewType) {
    data class ToggleAll(val areAllGranted: Boolean) : AuthorizedAppsItem(ViewType.TOGGLE_ALL)

    data class App(
        val appInfo: ApplicationInfo,
        val isGranted: Boolean,
        private val user: DeviceUser,
        private val label: String
    ) : AuthorizedAppsItem(ViewType.APP) {
        val packageName: String = appInfo.packageName!!
        val uid: Int = appInfo.uid
        val displayName: String =
            if (user.isCurrentUser) label
            else "$label (${user.name})"
    }

    enum class ViewType {
        TOGGLE_ALL,
        APP
    }
}