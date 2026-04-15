package moe.shizuku.manager.tcpmode

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSession
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.tcpmode.models.TcpState
import java.io.EOFException
import java.net.SocketException

class TcpManager(
    private val adbSettingsManager: AdbSettingsManager,
    private val adbPortHelper: AdbPortHelper,
    private val adbSessionFactory: AdbSession.Factory,
    preferencesRepository: PreferencesRepository
) {

    // TCP STATE

    private val refreshSignal = Channel<Unit>(Channel.CONFLATED)
    private val refreshFlow = refreshSignal.receiveAsFlow()

    val tcpState: Flow<TcpState> = combine(
        preferencesRepository.tcpMode.flow,
        preferencesRepository.tcpPort.flow,
        refreshFlow
    ) { tcpMode, targetPort, _ ->
        val currentPort = adbPortHelper.tcpPort

        TcpState(
            current =
                if (currentPort > 0) TcpState.Port.Enabled(currentPort)
                else TcpState.Port.Disabled,
            target =
                if (tcpMode) TcpState.Port.Enabled(targetPort)
                else TcpState.Port.Disabled
        )
    }

    fun refresh() {
        refreshSignal.trySend(Unit)
    }

    // TCP PORT CHECKS

    val isTcpPortOpen: Boolean
        get() = adbPortHelper.tcpPort > 0

    fun isTcpPortOpen(targetPort: Int): Boolean =
        adbPortHelper.tcpPort == targetPort

    // OPEN/CLOSE TCP PORT

    suspend fun openTcpPort(targetPort: Int) {
        if (isTcpPortOpen(targetPort)) return

        val currentPort = adbPortHelper.getAdbPort()

        adbSessionFactory.create(currentPort).use { session ->
            openTcpPort(targetPort, session)
        }
    }

    suspend fun openTcpPort(targetPort: Int, session: AdbSession): Result<Unit> = runCatching {
        if (isTcpPortOpen(targetPort)) return@runCatching

        session.withClient { client ->
            runCatching {
                client.command("tcpip:$targetPort")
            }.onFailure {
                if (it !is EOFException && it !is SocketException) throw it
            }
        }
        session.port = targetPort
        refresh()
    }.onFailure {
        Log.e(TAG, "Couldn't open TCP port", it)
        refresh()
        throw it
    }

    suspend fun closeTcpPort() {
        if (!isTcpPortOpen) return

        val tcpPort = adbPortHelper.tcpPort

        adbSessionFactory.create(tcpPort).use { session ->
            closeTcpPort(session)
        }
    }

    suspend fun closeTcpPort(session: AdbSession): Result<Unit> = runCatching {
        if (!isTcpPortOpen) return@runCatching

        check (adbSettingsManager.enableUsbDebugging().isSuccess)
        { "USB debugging not enabled" }

        with(session) {
            port = adbPortHelper.tcpPort
            withClient { client ->
                client.command("usb:")
            }
            port = 0
        }
        refresh()
    }.onFailure {
        if (isTcpPortOpen) {
            Log.e(TAG, "Couldn't close TCP port", it)
            refresh()
            throw it
        }
    }
}