package moe.shizuku.manager.core.platform.adb

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.extensions.isWifiConnected
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.KeyguardHelper
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AdbSettingsManager(
    private val context: Context,
    private val keyguardHelper: KeyguardHelper
) {
    // USB DEBUGGING

    val isUsbDebuggingEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) > 0

    fun enableUsbDebugging() {
        if (isUsbDebuggingEnabled) return

        val cr = context.contentResolver
        Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
        Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
    }

    // WIRELESS DEBUGGING

    val hasWirelessDebugging: Boolean
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
        get() = if (context.isTelevision) AndroidVersion.isAtLeast14
        else AndroidVersion.isAtLeast11

    val isWirelessDebuggingEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) > 0

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun enableWirelessDebugging(): Boolean {
        if (isWirelessDebuggingEnabled) return true

        if (!context.isWifiConnected) throw IllegalStateException("No wifi connection")
        Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)

        // Debounce to detect network not authorized for wireless debugging
        delay(100)

        return isWirelessDebuggingEnabled
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun enableWirelessDebuggingAwaitingAuth(
        onAwaitingAuth: (Boolean) -> Unit = {}
    ) {
        if (enableWirelessDebugging()) return

        onAwaitingAuth(true)

        if (keyguardHelper.isKeyguardLocked) {
            keyguardHelper.waitForUnlock()
            enableWirelessDebugging()
        }

        try {
            waitForWirelessDebuggingAuth()
        } finally {
            onAwaitingAuth(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun waitForWirelessDebuggingAuth() = suspendCancellableCoroutine { cont ->
        val cr = context.contentResolver
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                if (!cont.isActive) return
                cr.unregisterContentObserver(this)

                if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) > 0) {
                    cont.resume(Unit)
                } else {
                    cont.resumeWithException(
                        SecurityException("Wireless debugging authorization denied")
                    )
                }
            }
        }
        cr.registerContentObserver(Settings.Global.getUriFor("adb_wifi_enabled"), false, observer)
        cont.invokeOnCancellation {
            cr.unregisterContentObserver(observer)
        }
    }
}
