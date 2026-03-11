package moe.shizuku.manager.watchdog.models

sealed class WatchdogState {
    object Inactive : WatchdogState()
    object Active : WatchdogState()
    object Starting : WatchdogState()
    object Stopping : WatchdogState()
    data class Mismatch(val shouldBeRunning: Boolean) : WatchdogState()
}