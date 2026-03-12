package moe.shizuku.manager.settings.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.data.preferences.Preference
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.ui.LocaleHelper
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.settings.models.SettingsEvent
import moe.shizuku.manager.settings.models.SettingsUiState
import moe.shizuku.manager.utils.ShizukuStateMachine

class SettingsViewModel : ViewModel() {

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    private var pendingBatteryOptimization: Preference<*>? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        PreferencesRepository.startMode.flow,
        PreferencesRepository.startOnBoot.flow,
        PreferencesRepository.watchdog.flow,
        PreferencesRepository.tcpMode.flow,
        PreferencesRepository.tcpPort.flow,
        PreferencesRepository.theme.flow,
        PreferencesRepository.updateChannel.flow
    ) { args: Array<Any?> ->
        val prefs = Prefs(
            startMode = args[0] as StartMode,
            startOnBoot = args[1] as Boolean,
            watchdog = args[2] as Boolean,
            tcpMode = args[3] as Boolean,
            tcpPort = args[4] as Int,
            theme = args[5] as Theme,
            updateChannel = args[6] as UpdateChannel
        )
        calculateUiState(prefs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateUiState()
    )

    private data class Prefs(
        val startMode: StartMode = PreferencesRepository.startMode.value,
        val startOnBoot: Boolean = PreferencesRepository.startOnBoot.value,
        val watchdog: Boolean = PreferencesRepository.watchdog.value,
        val tcpMode: Boolean = PreferencesRepository.tcpMode.value,
        val tcpPort: Int = PreferencesRepository.tcpPort.value,
        val theme: Theme = PreferencesRepository.theme.value,
        val updateChannel: UpdateChannel = PreferencesRepository.updateChannel.value,
        val language: LocaleHelper.LocaleEntry = LocaleHelper.getLocale()
    )

    private fun calculateUiState(prefs: Prefs = Prefs()): SettingsUiState {
        val isTelevision = EnvironmentUtils.isTelevision()
        val isRooted = EnvironmentUtils.isRooted()
        val isTcpModeVisible = EnvironmentUtils.isTlsSupported()

        return SettingsUiState(
            startModeValue = prefs.startMode,
            isStartOnBootToggleable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || isTelevision || isRooted,
            startOnBootValue = PreferencesRepository.startOnBoot.value,
            watchdogValue = PreferencesRepository.watchdog.value,

            isWirelessDebuggingCategoryVisible = prefs.startMode == StartMode.WADB,
            isTcpModeVisible = isTcpModeVisible,
            tcpModeValue = prefs.tcpMode,
            isTcpPortVisible = isTcpModeVisible && prefs.tcpMode,
            tcpPortValue = PreferencesRepository.tcpPort.value,
            isLegacyPairingVisible = !isTelevision,

            languageValue = prefs.language,
            themeValue = prefs.theme,
            isAmoledBlackVisible = prefs.theme != Theme.LIGHT,
            isDynamicColorVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,

            updateChannelValue = PreferencesRepository.updateChannel.value
        )
    }

    fun getStartModeDescription(startMode: StartMode): Int? = when (startMode) {
        StartMode.WADB -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            R.string.wireless_debugging_requirement
        } else {
            R.string.wireless_debugging_requirement_pre_11
        }

        StartMode.ROOT -> null
    }

    fun getStartModeSelectable(startMode: StartMode): Boolean = when (startMode) {
        StartMode.WADB -> true
        StartMode.ROOT -> EnvironmentUtils.isRooted()
    }

    fun onStartModeChanged(newValue: StartMode) {
        PreferencesRepository.startMode.value = newValue
    }

    fun onStartOnBootChanged(newValue: Boolean) {
        if (shouldRequestBatteryOptimization(newValue)) {
            requestBatteryOptimization(PreferencesRepository.startOnBoot)
        }
        // https://r.android.com/2128832
        else if (!EnvironmentUtils.isTelevision() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            _events.trySend(SettingsEvent.ShowStartOnBootBugDialog)
        } else {
            applyStartOnBootChange(newValue)
        }
    }

    fun applyStartOnBootChange(newValue: Boolean) {
        PreferencesRepository.startOnBoot.value = newValue
    }

    fun onWatchdogChanged(newValue: Boolean) {
        if (shouldRequestBatteryOptimization(newValue)) {
            requestBatteryOptimization(PreferencesRepository.watchdog)
        } else {
            PreferencesRepository.watchdog.value = newValue
        }
    }

    fun onTcpModeChanged(newValue: Boolean) {
        val isTcpModeActive = EnvironmentUtils.getAdbTcpPort() > 0

        if (isTcpModeActive == newValue) {
            applyTcpModeChange(newValue)
            return
        }

        if (!newValue) {
            _events.trySend(SettingsEvent.PromptStopTcp)
        } else {
            if (ShizukuStateMachine.isRunning()) {
                _events.trySend(SettingsEvent.Snackbar(R.string.tcp_restarting_wifi))
            }
            applyTcpModeChange(true)
        }
    }

    fun applyTcpModeChange(newValue: Boolean) {
        PreferencesRepository.tcpMode.value = newValue
    }

    fun validatePort(input: String?): Int? {
        val port = input?.toIntOrNull()
        val isValid = (port == null) || (port in 1..65535)
        return if (!isValid) R.string.tcp_error_invalid_port else null
    }

    fun onTcpPortChanged(input: String) {
        val newPort = input.toIntOrNull() ?: PreferencesRepository.tcpPort.default
        val currentPort = EnvironmentUtils.getAdbTcpPort()
        val needsRestart = (currentPort != newPort)

        if (ShizukuStateMachine.isRunning() && needsRestart) {
            _events.trySend(SettingsEvent.Snackbar(R.string.tcp_restarting))
        } else {
            // TODO cancel pending restart
        }
        applyTcpPortChange(newPort)
    }

    fun applyTcpPortChange(newValue: Int) {
        PreferencesRepository.tcpPort.value = newValue
    }

    fun onThemeChanged(value: Theme) {
        if (PreferencesRepository.theme.value != value) {
            PreferencesRepository.theme.value = value
        }
    }

    fun onUpdateChannelChanged(value: UpdateChannel) {
        PreferencesRepository.updateChannel.value = value
    }

    // TODO remove context
    fun onStopTcp(context: Context) {
        viewModelScope.launch {
            AdbStarter.stopTcp(context, EnvironmentUtils.getAdbTcpPort())
            if (EnvironmentUtils.getAdbTcpPort() <= 0) {
                applyTcpModeChange(false)
            } else {
                context.toast(R.string.tcp_error_closing)
            }
        }
    }

    // Battery optimization logic
    private fun shouldRequestBatteryOptimization(settingsValue: Boolean) =
        settingsValue && !PowerManagerHelper.isIgnoringBatteryOptimizations() && !EnvironmentUtils.isTelevision()

    private fun requestBatteryOptimization(pref: Preference<*>) {
        pendingBatteryOptimization = pref
        _events.trySend(SettingsEvent.RequestBatteryOptimization)
    }

    fun onBatteryOptimizationResult() {
        val setting = pendingBatteryOptimization ?: return
        if (PowerManagerHelper.isIgnoringBatteryOptimizations()) {
            when (setting) {
                PreferencesRepository.startOnBoot -> onStartOnBootChanged(true)
                PreferencesRepository.watchdog -> onWatchdogChanged(true)
            }
        }
        pendingBatteryOptimization = null
    }
}
