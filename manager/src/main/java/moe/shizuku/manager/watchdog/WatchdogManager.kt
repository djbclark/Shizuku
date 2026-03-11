package moe.shizuku.manager.watchdog

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.watchdog.services.WatchdogService

object WatchdogManager {

    private val hasFailed = MutableStateFlow(false)
    private var transitionJob: Job? = null

    private lateinit var _state: StateFlow<WatchdogState>
    val state: StateFlow<WatchdogState> get() = _state

    fun init(context: Context, scope: CoroutineScope) {
        _state = combine(
            PreferencesRepository.observeWatchdog(),
            WatchdogService.isRunning,
            hasFailed
        ) { enabled, running, failed ->
            when {
                enabled && running -> WatchdogState.Active
                !enabled && !running -> WatchdogState.Inactive

                enabled && !failed -> WatchdogState.Starting
                !enabled && !failed -> WatchdogState.Stopping

                else -> WatchdogState.Mismatch
            }
        }.onEach { state ->
            when (state) {
                is WatchdogState.Starting -> transition(scope, context, WatchdogState.Active)
                is WatchdogState.Stopping -> transition(scope, context, WatchdogState.Inactive)
                else -> {}
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = WatchdogState.Inactive
        )
    }

    fun retry() {
        hasFailed.value = false
    }

    private fun transition(scope: CoroutineScope, context: Context, state: WatchdogState) {
        if (transitionJob?.isActive != true) {
            transitionJob = scope.launch {
                runCatching {
                    withTimeout(5000) {
                        _state.first { it == state }
                    }
                }.onFailure {
                    if (it is CancellationException) throw it
                    hasFailed.value = true
                }
            }
            when (state) {
                is WatchdogState.Active -> context.startWatchdogService()
                is WatchdogState.Inactive -> context.stopWatchdogService()
                else -> return
            }
        }
    }

    private fun Context.startWatchdogService() {
        try {
            val intent = Intent(this, WatchdogService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
            Log.e("WatchdogManager", "Failed to start service: ${e.message}")
            hasFailed.value = true
        }
    }

    private fun Context.stopWatchdogService() {
        try {
            val intent = Intent(this, WatchdogService::class.java)
            stopService(intent)
        } catch (e: Exception) {
            Log.e("WatchdogManager", "Failed to stop service: ${e.message}")
            hasFailed.value = true
        }
    }
}
