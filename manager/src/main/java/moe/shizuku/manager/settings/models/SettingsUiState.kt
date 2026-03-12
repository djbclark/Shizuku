package moe.shizuku.manager.settings.models

import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.core.ui.LocaleHelper

data class SettingsUiState(
    val startModeValue: StartMode,
    val isStartOnBootToggleable: Boolean,
    val startOnBootValue: Boolean,
    val watchdogValue: Boolean,

    val isWirelessDebuggingCategoryVisible: Boolean,
    val isTcpModeVisible: Boolean,
    val tcpModeValue: Boolean,
    val isTcpPortVisible: Boolean,
    val tcpPortValue: Int,
    val isLegacyPairingVisible: Boolean,

    val languageValue: LocaleHelper.LocaleEntry,
    val themeValue: Theme,
    val isAmoledBlackVisible: Boolean,
    val isDynamicColorVisible: Boolean,

    val updateChannelValue: UpdateChannel
)
