package moe.shizuku.manager.settings.models

import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.core.ui.LocaleHelper

data class SettingsUiState(
    val startModeValue: StartMode = PreferencesRepository.startMode.default,
    val isStartOnBootToggleable: Boolean = false,
    val startOnBootValue: Boolean = PreferencesRepository.startOnBoot.default,
    val watchdogValue: Boolean = PreferencesRepository.watchdog.default,

    val isWirelessDebuggingCategoryVisible: Boolean = false,
    val isTcpModeVisible: Boolean = false,
    val tcpModeValue: Boolean = PreferencesRepository.tcpMode.default,
    val isTcpPortVisible: Boolean = false,
    val tcpPortValue: Int = PreferencesRepository.tcpPort.default,
    val isLegacyPairingVisible: Boolean = false,

    val languageValue: LocaleHelper.LocaleEntry = LocaleHelper.systemDefaultEntry,
    val themeValue: Theme = PreferencesRepository.theme.default,
    val isAmoledBlackVisible: Boolean = false,
    val isDynamicColorVisible: Boolean = false,

    val updateChannelValue: UpdateChannel = PreferencesRepository.updateChannel.default
)
