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
        PreferencesRepository.all,
        LocaleHelper.localeFlow
    ) { _, _ ->
        calculateUiState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateUiState()
    )

    private fun calculateUiState(): SettingsUiState = with(PreferencesRepository) {
        val isTelevision = EnvironmentUtils.isTelevision()
        val isRooted = EnvironmentUtils.isRooted()
        val isTcpModeVisible = EnvironmentUtils.isTlsSupported()

        SettingsUiState(
            startModeValue = startMode.get(),
            isStartOnBootToggleable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || isTelevision || isRooted,
            startOnBootValue = startOnBoot.get(),
            watchdogValue = watchdog.get(),

            isWirelessDebuggingCategoryVisible = startMode.get() == StartMode.WADB,
            isTcpModeVisible = isTcpModeVisible,
            tcpModeValue = tcpMode.get(),
            isTcpPortVisible = isTcpModeVisible && tcpMode.get(),
            tcpPortValue = tcpPort.get(),
            isLegacyPairingVisible = !isTelevision,

            languageValue = LocaleHelper.getLocale(),
            themeValue = theme.get(),
            isAmoledBlackVisible = theme.get() != Theme.LIGHT,
            isDynamicColorVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,

            updateChannelValue = updateChannel.get()
        )
    }

    fun handleSelectionResult(value: Any) {
        when (value) {
            is StartMode -> onStartModeChanged(value)
            is LocaleHelper.LocaleEntry -> onLanguageChanged(value)
            is Theme -> onThemeChanged(value)
            is UpdateChannel -> onUpdateChannelChanged(value)
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

    private fun onStartModeChanged(newValue: StartMode) {
        PreferencesRepository.startMode.set(newValue)
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
        PreferencesRepository.startOnBoot.set(newValue)
    }

    fun onWatchdogChanged(newValue: Boolean) {
        if (shouldRequestBatteryOptimization(newValue)) {
            requestBatteryOptimization(PreferencesRepository.watchdog)
        } else {
            PreferencesRepository.watchdog.set(newValue)
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

    private fun applyTcpModeChange(newValue: Boolean) {
        PreferencesRepository.tcpMode.set(newValue)
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

    private fun applyTcpPortChange(newValue: Int) {
        PreferencesRepository.tcpPort.set(newValue)
    }

    private fun onLanguageChanged(newValue: LocaleHelper.LocaleEntry) {
        LocaleHelper.setLocale(newValue)
    }

    private fun onThemeChanged(value: Theme) {
        if (PreferencesRepository.theme.get() != value) {
            PreferencesRepository.theme.set(value)
        }
    }

    private fun onUpdateChannelChanged(value: UpdateChannel) {
        PreferencesRepository.updateChannel.set(value)
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
