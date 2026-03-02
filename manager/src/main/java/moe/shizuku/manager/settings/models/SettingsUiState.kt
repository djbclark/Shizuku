package moe.shizuku.manager.settings.models

import moe.shizuku.manager.core.data.preferences.PreferenceKeys
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel

data class SettingsUiState(
    val isStartOnBootToggleable: Boolean = false,
    val startOnBootValue: Boolean = PreferenceKeys.START_ON_BOOT.default,

    val watchdogValue: Boolean = PreferenceKeys.WATCHDOG.default,

    val isTcpModeVisible: Boolean = false,
    val tcpModeValue: Boolean = PreferenceKeys.TCP_MODE.default,

    val isTcpPortVisible: Boolean = false,
    val tcpPortValue: Int = PreferenceKeys.TCP_PORT.default,

    val isAutoDisableUsbDebuggingVisible: Boolean = false,

    val languageValue: String? = PreferenceKeys.LANGUAGE.default,
    val themeValue: Theme = PreferenceKeys.THEME.default,
    val isAmoledBlackVisible: Boolean = false,
    val isDynamicColorVisible: Boolean = false,

    val updateChannelValue: UpdateChannel = PreferenceKeys.UPDATE_CHANNEL.default,

    val isLegacyPairingVisible: Boolean = false
)