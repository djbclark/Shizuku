package moe.shizuku.manager.settings.models

import androidx.annotation.StringRes

// TODO add watchdog dialog for PC-only devices
sealed class SettingsEvent {
    data class Snackbar(@get:StringRes val msg: Int) : SettingsEvent()
    object PromptStopTcp : SettingsEvent()
    object ShowStartOnBootBugDialog : SettingsEvent()
    object RequestBatteryOptimization : SettingsEvent()
}