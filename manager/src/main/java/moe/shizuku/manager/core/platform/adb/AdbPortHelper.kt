package moe.shizuku.manager.core.platform.adb

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.extensions.isValidPort
import moe.shizuku.manager.core.platform.adb.models.AdbPortError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdbPortHelper(
    private val context: Context,
    private val adbMdns: AdbMdns,
    private val adbSettingsManager: AdbSettingsManager
) {
    suspend fun getAdbPort(forceTls: Boolean = false): Result<Int, AdbPortError> =
        tcpPort.takeIf { it.isOk && !forceTls }
            ?: if (adbSettingsManager.hasWirelessDebugging) getTlsPort()
            else Err(AdbPortError.NotFound)

    val tcpPort: Result<Int, AdbPortError>
        get() {
            val port = SystemProperties.getInt("service.adb.tcp.port", -1).takeIf { it.isValidPort }
                ?: SystemProperties.getInt("persist.adb.tcp.port", -1).takeIf { it.isValidPort }
                ?: if (context.isTelevision && !adbSettingsManager.hasWirelessDebugging) 5555 else null

            return port?.let { Ok(it) } ?: Err(AdbPortError.NotFound)
        }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun getTlsPort(): Result<Int, AdbPortError> = withContext(Dispatchers.IO) {
        withTimeoutOrNull(10_000L) {
            adbMdns.connectFlow.first()
                .let { Ok(it) }
        } ?: Err(AdbPortError.NotFound)
    }

}
