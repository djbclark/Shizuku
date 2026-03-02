package moe.shizuku.manager.settings.ui

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
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
import moe.shizuku.manager.core.data.KeyValueEntry
import moe.shizuku.manager.core.data.preferences.PreferenceKeys
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.core.ui.LocaleHelper
import moe.shizuku.manager.core.ui.components.toast
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.settings.models.SettingsEvent
import moe.shizuku.manager.settings.models.SettingsUiState
import moe.shizuku.manager.utils.ShizukuStateMachine

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pendingBatteryOptimization: KeyValueEntry<*>? = null

    init {
        updateUiState()
    }

    private fun updateUiState() {
        val isTelevision = EnvironmentUtils.isTelevision()
        val isRooted = EnvironmentUtils.isRooted()

        _uiState.update { state ->
            state.copy(
                isStartOnBootToggleable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        || isTelevision
                        || isRooted,
                startOnBootValue = PreferencesRepository.getStartOnBoot(),

                watchdogValue = PreferencesRepository.getWatchdog(),

                isTcpModeVisible = true,
                tcpModeValue = PreferencesRepository.getTcpMode(),

                isTcpPortVisible = true,
                tcpPortValue = PreferencesRepository.getTcpPort(),

                isAutoDisableUsbDebuggingVisible = true,

                languageValue = LocaleHelper.getLocaleDisplayName(),
                themeValue = PreferencesRepository.getTheme(),
                isAmoledBlackVisible = PreferencesRepository.getTheme() != Theme.LIGHT,
                isDynamicColorVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,

                updateChannelValue = PreferencesRepository.getUpdateChannel(),

                isLegacyPairingVisible = !isTelevision
            )
        }
    }

    fun onStartOnBootChanged(newValue: Boolean) {
        if (!newValue || PowerManagerHelper.isIgnoringBatteryOptimizations() || EnvironmentUtils.isTelevision()) {
            applyStartOnBootChange(newValue)
            return
        }

        // https://r.android.com/2128832
        if (!EnvironmentUtils.isTelevision() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            _events.trySend(SettingsEvent.ShowStartOnBootBugDialog)
        }

        pendingBatteryOptimization = PreferenceKeys.START_ON_BOOT
        _events.trySend(
            SettingsEvent.RequestBatteryOptimization
        )
    }

    fun applyStartOnBootChange(newValue: Boolean) {
        PreferencesRepository.setStartOnBoot(newValue)
        updateUiState()
    }

    fun onWatchdogChanged(newValue: Boolean) {
        if (newValue && shouldRequestBatteryOptimization()) {
            requestBatteryOptimization(PreferenceKeys.WATCHDOG)
        } else {
            PreferencesRepository.setWatchdog(newValue)
            updateUiState()
        }
    }

    fun onTcpModeChanged(newValue: Boolean) {
        val isTcpEnabled = EnvironmentUtils.getAdbTcpPort() > 0
        val isServiceRunning = ShizukuStateMachine.isRunning()

        if (isTcpEnabled == newValue) {
            applyTcpModeChange(newValue)
            return
        }

        when {
            // Service is running - restart required
            isServiceRunning -> {
                _events.trySend(SettingsEvent.PromptRestart(PreferenceKeys.TCP_MODE, true))
            }
            // Service is stopped, but user is disabling TCP mode
            !newValue -> {
                _events.trySend(SettingsEvent.PromptStopTcp)
            }
            // Service is stopped and user is enabling TCP mode
            else -> {
                applyTcpModeChange(true)
            }
        }
    }

    fun applyTcpModeChange(newValue: Boolean) {
        PreferencesRepository.setTcpMode(newValue)
        updateUiState()
    }

    fun onTcpPortChanged(newPort: Int) {
        val currentPort = EnvironmentUtils.getAdbTcpPort()
        val needsRestart = (currentPort > 0) && (currentPort != newPort)

        if (ShizukuStateMachine.isRunning() && needsRestart) {
            _events.trySend(SettingsEvent.PromptRestart(PreferenceKeys.TCP_PORT, newPort))
        } else {
            applyTcpPortChange(newPort)
        }
    }

    fun applyTcpPortChange(newValue: Int) {
        PreferencesRepository.setTcpPort(newValue)
        updateUiState()
    }

    // TODO remove theme changed listeners in favor of ThemeHelper
    fun onThemeChanged(value: Theme) {
        if (PreferencesRepository.getTheme() != value) {
            PreferencesRepository.setTheme(value)
            AppCompatDelegate.setDefaultNightMode(value.value)
            updateUiState()
            _events.trySend(SettingsEvent.RecreateActivity)
        }
    }

    fun onAmoledBlackChanged() {
        _events.trySend(SettingsEvent.RecreateActivity)
    }

    fun onDynamicColorChanged() {
        _events.trySend(SettingsEvent.RecreateActivity)
    }

    fun onUpdateChannelChanged(value: UpdateChannel) {
        PreferencesRepository.setUpdateChannel(value)
        updateUiState()
    }

    // TCP logic
    // TODO remove context
    fun onRestart(pref: KeyValueEntry<*>, newValue: Any, context: Context) {
        when (pref) {
            PreferenceKeys.TCP_MODE -> {
                applyTcpModeChange(newValue as Boolean)
            }

            PreferenceKeys.TCP_PORT -> {
                applyTcpPortChange(newValue as Int)
            }
        }
        ShizukuReceiverStarter.start(context, true)
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
    private fun shouldRequestBatteryOptimization() =
        !PowerManagerHelper.isIgnoringBatteryOptimizations() && !EnvironmentUtils.isTelevision()

    private fun requestBatteryOptimization(pref: KeyValueEntry<*>) {
        pendingBatteryOptimization = pref
        _events.trySend(SettingsEvent.RequestBatteryOptimization)
    }

    fun onBatteryOptimizationResult() {
        val setting = pendingBatteryOptimization ?: return
        if (PowerManagerHelper.isIgnoringBatteryOptimizations()) {
            when (setting) {
                PreferenceKeys.START_ON_BOOT -> PreferencesRepository.setStartOnBoot(true)
                PreferenceKeys.WATCHDOG -> PreferencesRepository.setWatchdog(true)
            }
        }
        pendingBatteryOptimization = null
    }
}
