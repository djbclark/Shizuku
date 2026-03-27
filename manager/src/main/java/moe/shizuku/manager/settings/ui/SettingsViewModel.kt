package moe.shizuku.manager.settings.ui

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
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.data.preferences.Preference
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.data.preferences.UpdateChannel
import moe.shizuku.manager.core.ui.helpers.LocaleHelper
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.settings.models.SettingsEvent
import moe.shizuku.manager.settings.models.SettingsUiState
import moe.shizuku.manager.shizukuservice.ShizukuServiceManager
import moe.shizuku.manager.utils.ShizukuStateMachine

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val localeHelper: LocaleHelper,
    private val powerManagerHelper: PowerManagerHelper,
    private val stateMachine: ShizukuStateMachine,
    private val shizukuServiceManager: ShizukuServiceManager,
    private val environmentUtils: EnvironmentUtils
) : ViewModel() {

    private val _events = Channel<SettingsEvent>()
    val events = _events.receiveAsFlow()

    private var pendingBatteryOptimization: Preference<*>? = null

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.all,
        localeHelper.localeFlow
    ) { _, _ ->
        calculateUiState()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateUiState()
    )

    private fun calculateUiState(): SettingsUiState = with(preferencesRepository) {
        val isTelevision = environmentUtils.isTelevision()
        val isRooted = EnvironmentUtils.isRooted()
        val isTcpModeVisible = environmentUtils.isTlsSupported()

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

            languageValue = localeHelper.getLocale(),
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

    private fun onStartModeChanged(newValue: StartMode) {
        preferencesRepository.startMode.set(newValue)
    }

    fun onStartOnBootChanged(newValue: Boolean) {
        if (shouldRequestBatteryOptimization(newValue)) {
            requestBatteryOptimization(preferencesRepository.startOnBoot)
        }
        else if (!environmentUtils.isTelevision() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            _events.trySend(SettingsEvent.ShowStartOnBootBugDialog)
        } else {
            applyStartOnBootChange(newValue)
        }
    }

    fun applyStartOnBootChange(newValue: Boolean) {
        preferencesRepository.startOnBoot.set(newValue)
    }

    fun onWatchdogChanged(newValue: Boolean) {
        if (shouldRequestBatteryOptimization(newValue)) {
            requestBatteryOptimization(preferencesRepository.watchdog)
        } else {
            preferencesRepository.watchdog.set(newValue)
        }
    }

    fun onTcpModeChanged(newValue: Boolean) {
        val isTcpModeActive = environmentUtils.getAdbTcpPort() > 0

        if (isTcpModeActive == newValue) {
            applyTcpModeChange(newValue)
            return
        }

        if (!newValue) {
            _events.trySend(SettingsEvent.PromptStopTcp)
        } else {
            if (stateMachine.isRunning()) {
                _events.trySend(SettingsEvent.Snackbar(R.string.tcp_restarting_wifi))
            }
            applyTcpModeChange(true)
        }
    }

    private fun applyTcpModeChange(newValue: Boolean) {
        preferencesRepository.tcpMode.set(newValue)
    }

    fun validatePort(input: String?): Int? {
        val port = input?.toIntOrNull()
        val isValid = (port == null) || (port in 1..65535)
        return if (!isValid) R.string.tcp_error_invalid_port else null
    }

    fun onTcpPortChanged(input: String) {
        val newPort = input.toIntOrNull() ?: preferencesRepository.tcpPort.default
        val currentPort = environmentUtils.getAdbTcpPort()
        val needsRestart = (currentPort != newPort)

        if (stateMachine.isRunning() && needsRestart) {
            _events.trySend(SettingsEvent.Snackbar(R.string.tcp_restarting))
        }
        applyTcpPortChange(newPort)
    }

    private fun applyTcpPortChange(newValue: Int) {
        preferencesRepository.tcpPort.set(newValue)
    }

    private fun onLanguageChanged(newValue: LocaleHelper.LocaleEntry) {
        localeHelper.setLocale(newValue)
    }

    private fun onThemeChanged(value: Theme) {
        if (preferencesRepository.theme.get() != value) {
            preferencesRepository.theme.set(value)
        }
    }

    private fun onUpdateChannelChanged(value: UpdateChannel) {
        preferencesRepository.updateChannel.set(value)
    }

    fun onStopTcp() {
        viewModelScope.launch {
            shizukuServiceManager.stopTcp()
            if (environmentUtils.getAdbTcpPort() <= 0) {
                applyTcpModeChange(false)
            } else {
                _events.trySend(SettingsEvent.Snackbar(R.string.tcp_error_closing))
            }
        }
    }

    private fun shouldRequestBatteryOptimization(settingsValue: Boolean) =
        settingsValue && !powerManagerHelper.isIgnoringBatteryOptimizations() && !environmentUtils.isTelevision()

    private fun requestBatteryOptimization(pref: Preference<*>) {
        pendingBatteryOptimization = pref
        _events.trySend(SettingsEvent.RequestBatteryOptimization)
    }

    fun onBatteryOptimizationResult() {
        val setting = pendingBatteryOptimization ?: return
        if (powerManagerHelper.isIgnoringBatteryOptimizations()) {
            when (setting) {
                preferencesRepository.startOnBoot -> onStartOnBootChanged(true)
                preferencesRepository.watchdog -> onWatchdogChanged(true)
            }
        }
        pendingBatteryOptimization = null
    }
}
