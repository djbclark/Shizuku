package moe.shizuku.manager.core.adb

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.shizuku.manager.core.adb.client.AdbMdns
import moe.shizuku.manager.core.extensions.isTelevision
import kotlin.coroutines.resume

class AdbPortHelper(
    private val context: Context,
    private val adbSettingsManager: AdbSettingsManager
) {
    suspend fun getAdbPort(forceTls: Boolean = false): Int =
        tcpPort.takeUnless { it <= 0 || forceTls }
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getTlsPort()
            else throw IllegalStateException("ADB port not found")

    val tcpPort: Int
        get() = SystemProperties.getInt("service.adb.tcp.port", -1).takeUnless { it == -1 }
            ?: SystemProperties.getInt("persist.adb.tcp.port", -1).takeUnless { it == -1 }
            ?: if (context.isTelevision && !adbSettingsManager.hasWirelessDebugging) 5555 else -1

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun getTlsPort(): Int {
        adbSettingsManager.enableWirelessDebuggingAwaitingAuth()

        return suspendCancellableCoroutine { cont ->
            var mdns: AdbMdns? = null
            mdns = AdbMdns(context, AdbMdns.TLS_CONNECT) { port ->
                if (port in 1..65535) {
                    mdns?.stop()
                    if (cont.isActive) cont.resume(port)
                }
            }
            mdns.start()
            cont.invokeOnCancellation {
                mdns.stop()
            }
        }
    }
}
