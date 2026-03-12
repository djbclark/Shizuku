package moe.shizuku.manager.watchdog

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.watchdog.models.WatchdogState
import moe.shizuku.manager.watchdog.services.WatchdogService

object WatchdogManager {

    private val hasFailed = MutableStateFlow(false)
    private var transitionJob: Job? = null

    private val _state = MutableStateFlow<WatchdogState>(WatchdogState.Inactive)
    val state: StateFlow<WatchdogState> get() = _state

    fun init(context: Context, scope: CoroutineScope) {
        scope.launch {
            combine(
                PreferencesRepository.watchdog.flow, WatchdogService.isRunning, hasFailed
            ) { enabled, running, failed ->
                when {
                    enabled && running -> WatchdogState.Active
                    !enabled && !running -> WatchdogState.Inactive

                    enabled && !failed -> WatchdogState.Starting
                    !enabled && !failed -> WatchdogState.Stopping

                    enabled && !running -> WatchdogState.Mismatch(shouldBeRunning = true)
                    else -> WatchdogState.Mismatch(shouldBeRunning = false)
                }
            }.distinctUntilChanged().collect { newState ->
                _state.value = newState

                when (newState) {
                    is WatchdogState.Active, is WatchdogState.Inactive ->
                        hasFailed.value = false

                    is WatchdogState.Starting ->
                        transition(
                            scope, context, WatchdogState.Active
                        )

                    is WatchdogState.Stopping ->
                        transition(
                            scope, context, WatchdogState.Inactive
                        )

                    else -> {}
                }
            }
        }
    }

    fun retry() {
        hasFailed.value = false
    }

    private fun transition(scope: CoroutineScope, context: Context, targetState: WatchdogState) {
        transitionJob?.cancel()
        transitionJob = scope.launch {
            runCatching {
                withTimeout(5000) {
                    state.first { it == targetState }
                }
            }.onFailure {
                if (it is CancellationException) throw it
                hasFailed.value = true
            }
        }
        when (targetState) {
            is WatchdogState.Active -> context.startWatchdogService()
            is WatchdogState.Inactive -> context.stopWatchdogService()
            else -> return
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
