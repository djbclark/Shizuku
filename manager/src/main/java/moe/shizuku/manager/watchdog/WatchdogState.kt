package moe.shizuku.manager.watchdog

sealed class WatchdogState {
    object Inactive : WatchdogState()
    object Starting : WatchdogState()
    object Active : WatchdogState()
    object Mismatch : WatchdogState()
    object Stopping : WatchdogState()
}