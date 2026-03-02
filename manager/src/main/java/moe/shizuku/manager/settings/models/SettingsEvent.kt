package moe.shizuku.manager.settings.models

import moe.shizuku.manager.core.data.KeyValueEntry

// TODO add watchdog dialog for PC-only devices
sealed class SettingsEvent {
    data class PromptRestart(val setting: KeyValueEntry<*>, val newValue: Any) : SettingsEvent()
    object PromptStopTcp : SettingsEvent()
    object ShowStartOnBootBugDialog : SettingsEvent()
    object RequestBatteryOptimization : SettingsEvent()
    object RecreateActivity : SettingsEvent()
}