package moe.shizuku.manager.privilegedservice

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onErr
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSession
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.platform.adb.models.AdbSettingsError
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.preferences.models.StartMode
import moe.shizuku.manager.core.utils.root.RootUtils
import moe.shizuku.manager.core.utils.runnable.RunnableSequence
import moe.shizuku.manager.core.utils.runnable.RunnableSequenceError
import moe.shizuku.manager.start.StartStep
import moe.shizuku.manager.start.models.PreStartCheckError
import moe.shizuku.manager.tcpmode.TcpManager
import rikka.shizuku.Shizuku
import java.io.File

class PrivilegedServiceManager(
    private val context: Context,
    private val adbSettingsManager: AdbSettingsManager,
    private val adbPortHelper: AdbPortHelper,
    private val adbSessionFactory: AdbSession.Factory,
    private val preferencesRepository: PreferencesRepository,
    private val privilegedServiceStateMachine: PrivilegedServiceStateMachine,
    private val tcpManager: TcpManager
) {
    private val starterFilePath by lazy {
        File(context.applicationInfo.nativeLibraryDir, "libshizuku.so").absolutePath
    }
    private val internalCommand by lazy {
        "$starterFilePath --apk=${context.applicationInfo.sourceDir}"
    }
    val adbCommand: String by lazy { "adb shell $starterFilePath" }

    val isWifiRequired: Boolean
        get() {
            val isStartModeWadb = preferencesRepository.startMode.get() == StartMode.WADB
            val isTcpPortOpen = tcpManager.isTcpPortOpen
            val isTcpModeEnabled = preferencesRepository.tcpMode.get()

            return isStartModeWadb && (isTcpPortOpen || !isTcpModeEnabled)
        }

    fun checkBackgroundStart(): Result<Unit, PreStartCheckError.Background> =
        checkStartCommon().fold(
            failure = { Err(it as PreStartCheckError.Background) },
            success = {
                if (context.hasWriteSecureSettings()) Ok(Unit)
                else Err(PreStartCheckError.WriteSecureSettingsNotGranted)
            }
        ).also { Log.i(TAG, "checkBackgroundStart: $it") }

    suspend fun checkForegroundStart(): Result<Unit, PreStartCheckError.Foreground> =
        checkStartCommon().fold(
            failure = { Err(it as PreStartCheckError.Foreground) },
            success = {
                adbSettingsManager.setUsbDebugging(true)
                    .mapError { PreStartCheckError.UsbDebuggingDisabled }
                    .onErr { return Err(it) }

                @SuppressLint("NewApi") // Already checked in canStartCommon
                return adbSettingsManager.setWirelessDebugging(true)
                    .mapError {
                        when (it) {
                            AdbSettingsError.NotSupported -> PreStartCheckError.TlsNotSupported
                            AdbSettingsError.NoWifi -> PreStartCheckError.WifiRequired
                            AdbSettingsError.NotAuthorized -> PreStartCheckError.AuthorizationRequired
                            AdbSettingsError.NoWriteSecureSettings -> PreStartCheckError.WirelessDebuggingDisabled
                        }
                    }
            }
        ).also { Log.i(TAG, "checkForegroundStart: $it") }

    private fun checkStartCommon(): Result<Unit, PreStartCheckError> =
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                if (RootUtils.isRooted() == true) Ok(Unit) // TODO handle null
                else Err(PreStartCheckError.NotRooted)
            }

            StartMode.WADB -> {
                if (adbPortHelper.tcpPort.isOk) Ok(Unit)
                else if (!adbSettingsManager.hasWirelessDebugging) Err(PreStartCheckError.TlsNotSupported)
                else Ok(Unit)
            }
        }

    fun createStartSession(): RunnableSequence<StartStep<*, *>> {
        val steps = mutableListOf<StartStep<*, *>>()
        val startMode = preferencesRepository.startMode.get()
        var adbSession: AdbSession? = null

        if (startMode == StartMode.WADB) {
            adbSession = adbSessionFactory.create()
        }

        when (startMode) {
            StartMode.ROOT -> {
                steps.add(StartStep.RequestRootPermission())
            }

            StartMode.WADB -> {
                val tcpMode = preferencesRepository.tcpMode.get()
                val targetTcpPort = preferencesRepository.tcpPort.get()
                val session = adbSession!!

                if (!tcpMode && tcpManager.isTcpPortOpen) {
                    steps.add(StartStep.CloseTcpPort(session, tcpManager))
                }

                if (!adbSettingsManager.isUsbDebuggingEnabled) {
                    steps.add(StartStep.EnableUsbDebugging(adbSettingsManager))
                }

                if (adbSettingsManager.hasWirelessDebugging &&
                    !adbSettingsManager.isWirelessDebuggingEnabled &&
                    isWifiRequired
                ) {
                    steps.add(StartStep.EnableWirelessDebugging(adbSettingsManager))
                }

                steps.add(StartStep.SearchForPort(session, adbPortHelper, isWifiRequired))
                steps.add(StartStep.ConnectToPort(session))

                if (tcpMode && !tcpManager.isTcpPortOpen(targetTcpPort)) {
                    steps.add(StartStep.OpenTcpPort(session, tcpManager, targetTcpPort))
                }
            }
        }

        steps.add(StartStep.ExecuteCommand(adbSession, startMode, internalCommand))
        steps.add(StartStep.WaitForService(privilegedServiceStateMachine))

        return RunnableSequence(steps.toList())
    }

    suspend fun startService(sequence: RunnableSequence<StartStep<*, *>>): Result<Unit, RunnableSequenceError> {
        privilegedServiceStateMachine.setStarting()
        return sequence.run().also {
            privilegedServiceStateMachine.refresh()
        }
    }

    fun stopService() {
        if (!privilegedServiceStateMachine.isRunning) return

        privilegedServiceStateMachine.setStoppping()
        resultOf { Shizuku.exit() } // TODO catch specific error
    }

}