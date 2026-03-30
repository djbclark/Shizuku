package moe.shizuku.manager.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import rikka.shizuku.Shizuku
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class ShizukuStateMachine(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {

    enum class State { STARTING, RUNNING, STOPPING, STOPPED, CRASHED }

    private var state = AtomicReference<State>(State.STOPPED)
    private val listeners = CopyOnWriteArrayList<(State) -> Unit>()

    init {
        Shizuku.addBinderReceivedListenerSticky {
            set(State.RUNNING)
        }
        Shizuku.addBinderDeadListener {
            setDead()
        }
    }

    fun get(): State = state.get()

    private fun transition(transform: (State) -> State) {
        val oldState = state.getAndUpdate(transform)
        val newState = transform(oldState)
        if (oldState != newState) {
            listeners.forEach { it(newState) }
            Log.d("ShizukuStateMachine", newState.toString())
        }
    }

    fun set(newState: State) = transition { newState }

    fun setDead() = transition {
        when (it) {
            State.RUNNING -> State.CRASHED
            State.STOPPING -> {
                try {
                    val permissionGranted = context.hasWriteSecureSettings()
                    val shouldDisableUsbDebugging =
                        permissionGranted && preferencesRepository.autoDisableUsbDebugging.get()
                    if (shouldDisableUsbDebugging) {
                        Settings.Global.putInt(
                            context.contentResolver,
                            Settings.Global.ADB_ENABLED,
                            0
                        )
                    }
                } catch (e: Exception) {
                    Log.w("ShizukuStateMachine", "Failed to disable USB debugging", e)
                }
                State.STOPPED
            }

            else -> it
        }
    }

    fun update(): State {
        val state = if (Shizuku.pingBinder()) State.RUNNING else State.STOPPED
        set(state)
        return state
    }

    fun isRunning(): Boolean {
        return get() == State.RUNNING
    }

    fun isDead(): Boolean {
        return (get() == State.STOPPED || get() == State.CRASHED)
    }

    fun addListener(listener: (State) -> Unit) {
        listeners.add(listener)
        listener(state.get())
    }

    fun removeListener(listener: (State) -> Unit) {
        listeners.remove(listener)
    }

    fun asFlow(): Flow<State> = callbackFlow {
        val listener: (State) -> Unit = { trySend(it) }
        addListener(listener)
        awaitClose { removeListener(listener) }
    }

}
