package moe.shizuku.manager.watchdog.services

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.core.data.preferences.PreferencesRepository

object WatchdogManager {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // TODO add canRestart function to check if service can be restarted by watchdog

    fun init(context: Context, scope: CoroutineScope) {
        _isRunning.value = WatchdogService.isRunning()
        PreferencesRepository.observeWatchdog().onEach {
            if (it) {
                WatchdogService.start(context)
            } else {
                WatchdogService.stop(context)
            }
            _isRunning.value = WatchdogService.isRunning()
        }.launchIn(scope)
    }
}