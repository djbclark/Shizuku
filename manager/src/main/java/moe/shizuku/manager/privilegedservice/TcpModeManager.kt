package moe.shizuku.manager.privilegedservice

import android.util.Log
import moe.shizuku.manager.core.adb.AdbManager
import moe.shizuku.manager.core.adb.AdbSession
import moe.shizuku.manager.core.extensions.TAG
import java.io.EOFException
import java.net.SocketException

class TcpModeManager(
    private val adbManager: AdbManager,
    private val adbSessionFactory: AdbSession.AdbSessionFactory
) {

    suspend fun openTcpPort(targetPort: Int) {
        if (isTcpPortOpen(targetPort)) return

        val currentPort = adbManager.getAdbPort(forceTls = false)

        adbSessionFactory.create(currentPort).use { session ->
            openTcpPort(targetPort, session)
        }
    }

    suspend fun openTcpPort(targetPort: Int, session: AdbSession) = runCatching {
        if (isTcpPortOpen(targetPort)) return@runCatching

        session.withClient { client ->
            runCatching {
                client.command("tcpip:$targetPort")
            }.onFailure {
                if (it !is EOFException && it !is SocketException) throw it
            }
        }
        session.port = targetPort
    }.onFailure {
        Log.e(TAG, "Couldn't open TCP port", it)
        throw it
    }

    suspend fun closeTcpPort() {
        if (!isTcpPortOpen) return

        val tcpPort = adbManager.getTcpPort()

        adbSessionFactory.create(tcpPort).use { session ->
            closeTcpPort(session)
        }
    }

    suspend fun closeTcpPort(session: AdbSession) = runCatching {
        if (!isTcpPortOpen) return@runCatching

        adbManager.enableUsbDebugging()

        with(session) {
            port = adbManager.getTcpPort()
            withClient { client ->
                client.command("usb:")
            }
            port = 0
        }
    }.onFailure {
        if (isTcpPortOpen) {
            Log.e(TAG, "Couldn't close TCP port", it)
            throw it
        }
    }

    val isTcpPortOpen: Boolean
        get() = adbManager.getTcpPort() > 0

    fun isTcpPortOpen(targetPort: Int) =
        adbManager.getTcpPort() == targetPort
}