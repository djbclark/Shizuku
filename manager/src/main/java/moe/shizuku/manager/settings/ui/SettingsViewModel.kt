package moe.shizuku.manager.settings.ui

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    private var pendingBatteryOptimization: Preference<*>? = null

    init {
        updateUiState()
    }

    private fun updateUiState() {
        val isTelevision = EnvironmentUtils.isTelevision()
        val isRooted = EnvironmentUtils.isRooted()
        val startMode = PreferencesRepository.startMode.value

        val isTcpModeVisible = EnvironmentUtils.isTlsSupported()
        val isTcpModeEnabled = PreferencesRepository.tcpMode.value

        _uiState.update { state ->
            state.copy(
                startModeValue = startMode,
                isStartOnBootToggleable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || isTelevision || isRooted,
                startOnBootValue = PreferencesRepository.startOnBoot.value,
                watchdogValue = PreferencesRepository.watchdog.value,

                isWirelessDebuggingCategoryVisible = startMode == StartMode.WADB,
                isTcpModeVisible = isTcpModeVisible,
                tcpModeValue = isTcpModeEnabled,
                isTcpPortVisible = isTcpModeVisible && isTcpModeEnabled,
                tcpPortValue = PreferencesRepository.tcpPort.value,
                isLegacyPairingVisible = !isTelevision,

                languageValue = LocaleHelper.getLocale(),
                themeValue = PreferencesRepository.theme.value,
                isAmoledBlackVisible = PreferencesRepository.theme.value != Theme.LIGHT,
                isDynamicColorVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,

                updateChannelValue = PreferencesRepository.updateChannel.value
            )
        }
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
        updateUiState()
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
        updateUiState()
    }

    fun onWatchdogChanged(newValue: Boolean) {
        if (shouldRequestBatteryOptimization(newValue)) {
            requestBatteryOptimization(PreferencesRepository.watchdog)
        } else {
            PreferencesRepository.watchdog.value = newValue
            updateUiState()
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
        updateUiState()
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
        updateUiState()
    }

    // TODO remove theme changed listeners in favor of ThemeHelper
    fun onThemeChanged(value: Theme) {
        if (PreferencesRepository.theme.value != value) {
            PreferencesRepository.theme.value = value
            updateUiState()
        }
    }

    fun onUpdateChannelChanged(value: UpdateChannel) {
        PreferencesRepository.updateChannel.value = value
        updateUiState()
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
        settingsValue &&
                !PowerManagerHelper.isIgnoringBatteryOptimizations() &&
                !EnvironmentUtils.isTelevision()

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
