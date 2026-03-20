package moe.shizuku.manager.home.models

sealed class HomeEvent {
    data class OpenUrl(val url: String) : HomeEvent()
    object ShowRebootDialog : HomeEvent()
    object ShowUninstallDialog : HomeEvent()
    object ShowBatteryOptimizationSnackbar : HomeEvent()
}
