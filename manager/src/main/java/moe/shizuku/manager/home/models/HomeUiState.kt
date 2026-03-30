package moe.shizuku.manager.home.models

import moe.shizuku.manager.privilegedservice.models.ServiceStatus
import moe.shizuku.manager.watchdog.models.WatchdogState

data class HomeUiState(
    val serviceState: ServiceStatus,
    val watchdogState: WatchdogState,
    val isStealthModeActive: Boolean,
    val authorizedAppsCount: Int
)