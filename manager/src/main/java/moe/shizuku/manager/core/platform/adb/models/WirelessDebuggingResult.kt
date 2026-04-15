package moe.shizuku.manager.core.platform.adb.models

sealed interface WirelessDebuggingResult {
    data object Success : WirelessDebuggingResult
    data object NotSupported : WirelessDebuggingResult
    data object NoWifi : WirelessDebuggingResult
    data object NoWriteSecureSettings : WirelessDebuggingResult
    data object NotAuthorized : WirelessDebuggingResult
}