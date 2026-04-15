package moe.shizuku.manager.core.platform.adb

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.provider.Settings
import androidx.annotation.CheckResult
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.extensions.isWifiConnected
import moe.shizuku.manager.core.platform.adb.models.WirelessDebuggingResult
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.platform.services.KeyguardHelper
import kotlin.coroutines.resume

class AdbSettingsManager(
    private val context: Context,
    private val keyguardHelper: KeyguardHelper
) {
    // USB DEBUGGING

    val isUsbDebuggingEnabled: Boolean
        get() = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) > 0

    @CheckResult
    fun enableUsbDebugging() : Result<Unit> {
        if (isUsbDebuggingEnabled) return Result.success(Unit)

        val cr = context.contentResolver
        try {
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        } catch (e: SecurityException) {
            return Result.failure(e)
        }

        return Result.success(Unit)
    }

    // WIRELESS DEBUGGING

    val hasWirelessDebugging: Boolean
        @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
        get() = if (context.isTelevision) AndroidVersion.isAtLeast14
        else AndroidVersion.isAtLeast11

    val isWirelessDebuggingEnabled: Boolean
        @RequiresApi(Build.VERSION_CODES.R)
        get() = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) > 0

    @RequiresApi(Build.VERSION_CODES.R)
    @CheckResult
    suspend fun enableWirelessDebugging(
        onAwaitingAuth: (Boolean) -> Unit = {}
    ): WirelessDebuggingResult {
        if (!hasWirelessDebugging) return WirelessDebuggingResult.NotSupported

        enableWirelessDebuggingInternal()?.let { result -> return result }

        onAwaitingAuth(true)

        if (keyguardHelper.isKeyguardLocked) {
            keyguardHelper.waitForUnlock()
            enableWirelessDebuggingInternal()?.let { result -> return result }
        }

        return awaitAuthResult().also {
            onAwaitingAuth(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @CheckResult
    private suspend fun enableWirelessDebuggingInternal(): WirelessDebuggingResult? {
        if (isWirelessDebuggingEnabled) return WirelessDebuggingResult.Success
        if (!context.isWifiConnected) return WirelessDebuggingResult.NoWifi

        try {
            Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 1)
        } catch (_: SecurityException) {
            return WirelessDebuggingResult.NoWriteSecureSettings
        }

        // Debounce to detect network not authorized for wireless debugging
        delay(100)
        return if (isWirelessDebuggingEnabled) WirelessDebuggingResult.Success
        else null // Non-terminal state, need to wait for auth result
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @CheckResult
    private suspend fun awaitAuthResult(): WirelessDebuggingResult =
        suspendCancellableCoroutine { cont ->
            val cr = context.contentResolver
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    cr.unregisterContentObserver(this)
                    if (!cont.isActive) return

                    if (isWirelessDebuggingEnabled) cont.resume(WirelessDebuggingResult.Success)
                    else cont.resume(WirelessDebuggingResult.NotAuthorized)
                }
            }
            cr.registerContentObserver(
                Settings.Global.getUriFor("adb_wifi_enabled"),
                false,
                observer
            )
            cont.invokeOnCancellation {
                cr.unregisterContentObserver(observer)
            }
        }
}
