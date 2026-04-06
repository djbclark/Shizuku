package moe.shizuku.manager.permission.models

import androidx.annotation.StringRes

sealed class AuthorizedAppsEvent {
    data object NotifyAdbRestricted : AuthorizedAppsEvent()
    data class ShowError(@param:StringRes val error: Int) : AuthorizedAppsEvent()

    enum class Dialog {
        ADB_RESTRICTED
    }
}