package moe.shizuku.manager.core.adb

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.shizuku.manager.core.android.DeviceHelper
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.extensions.isWifiConnected
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AdbSettingsManager(
    private val context: Context,
    private val deviceHelper: DeviceHelper
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
        get() = if (context.isTelevision) Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        else Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val isWirelessDebuggingEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) > 0

    @RequiresApi(Build.VERSION_CODES.R)
    fun enableWirelessDebugging(): Boolean {
        if (isWirelessDebuggingEnabled) return true

        if (!context.isWifiConnected) throw IllegalStateException("No wifi connection")
        Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)

        return isWirelessDebuggingEnabled
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun enableWirelessDebuggingAwaitingAuth() {
        if (enableWirelessDebugging()) return

        if (deviceHelper.isKeyguardLocked) {
            deviceHelper.waitForUnlock()
            enableWirelessDebugging()
        }

        waitForWirelessDebuggingAuth()
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
