package moe.shizuku.manager.permission.models

import android.content.pm.ApplicationInfo

sealed class AuthorizedAppsItem(val viewType: ViewType) {
    data class ToggleAll(val areAllGranted: Boolean) : AuthorizedAppsItem(ViewType.TOGGLE_ALL)

    data class App(
        val appInfo: ApplicationInfo,
        val displayName: String,
        val packageName: String,
        val isGranted: Boolean
    ) : AuthorizedAppsItem(ViewType.APP)

    enum class ViewType {
        TOGGLE_ALL,
        APP
    }
}