package moe.shizuku.manager.privilegedservice.models

import androidx.annotation.StringRes
import moe.shizuku.manager.R

sealed class PreStartCheck {
    object Success : PreStartCheck()

    sealed class Failure(@param:StringRes val msgRes: Int) : PreStartCheck() {
        object NotRooted :
            Failure(R.string.start_error_root)

        object TlsNotSupported :
            Failure(R.string.start_error_tls_not_supported)

        object WriteSecureSettingsNotGranted :
            Failure(R.string.start_error_write_secure_settings)

        object UsbDebuggingDisabled :
            Failure(R.string.start_error_usb_debugging_disabled)

        object WirelessDebuggingDisabled :
            Failure(R.string.start_error_wireless_debugging_disabled)

        object WifiRequired :
            Failure(R.string.start_error_wifi_required)

        object AuthorizationRequired :
            Failure(R.string.start_error_authorization_required)
    }
}