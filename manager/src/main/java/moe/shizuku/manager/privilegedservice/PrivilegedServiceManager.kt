package moe.shizuku.manager.privilegedservice

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSession
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.preferences.models.StartMode
import moe.shizuku.manager.core.utils.RootUtils
import moe.shizuku.manager.core.utils.runnable.RunnableSequence
import moe.shizuku.manager.privilegedservice.data.ShizukuStateMachine
import moe.shizuku.manager.privilegedservice.models.PreStartCheck
import moe.shizuku.manager.privilegedservice.models.StartStep
import moe.shizuku.manager.tcpmode.TcpManager
import rikka.shizuku.Shizuku
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
class PrivilegedServiceManager(
    private val context: Context,
    private val adbSettingsManager: AdbSettingsManager,
    private val adbPortHelper: AdbPortHelper,
    private val tcpManager: TcpManager,
    private val preferencesRepository: PreferencesRepository,
    private val shizukuStateMachine: ShizukuStateMachine,
    private val adbSessionFactory: AdbSession.Factory
) {
    private val starterFilePath by lazy {
        File(context.applicationInfo.nativeLibraryDir, "libshizuku.so").absolutePath
    }
    private val internalCommand by lazy {
        "$starterFilePath --apk=${context.applicationInfo.sourceDir}"
    }
    val adbCommand: String by lazy { "adb shell $starterFilePath" }

    private var adbSession: AdbSession? = null

    val isWifiRequired: Boolean
        get() = preferencesRepository.startMode.get() == StartMode.WADB &&
                (adbPortHelper.tcpPort <= 0 || !preferencesRepository.tcpMode.get())

    fun canStartInBackground(): PreStartCheck =
        canStartCommon() ?: run {
            if (context.hasWriteSecureSettings()) PreStartCheck.Success
            else PreStartCheck.Failure.WriteSecureSettingsNotGranted
        }.also { Log.i(TAG, "canStartInBackground: ${it::class.simpleName}") }

    suspend fun canStart(): PreStartCheck =
        canStartCommon() ?: run {
            runCatching { adbSettingsManager.enableUsbDebugging() }
                .onFailure { return@run PreStartCheck.Failure.UsbDebuggingDisabled }

            // Repeated to satisfy NewApi lint check, canStartCommon already checks this
            if (!adbSettingsManager.hasWirelessDebugging)
                return PreStartCheck.Failure.TlsNotSupported

            runCatching { adbSettingsManager.enableWirelessDebugging() }
                .fold(
                    onSuccess = { enabled ->
                        if (enabled) PreStartCheck.Success
                        else PreStartCheck.Failure.AuthorizationRequired
                    },
                    onFailure = {
                        if (it is IllegalStateException) PreStartCheck.Failure.WifiRequired
                        else PreStartCheck.Failure.WirelessDebuggingDisabled
                    }
                )
        }.also { Log.i(TAG, "canStart: ${it::class.simpleName}") }

    private fun canStartCommon(): PreStartCheck? =
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                if (RootUtils.isRooted()) PreStartCheck.Success
                else PreStartCheck.Failure.NotRooted
            }

            StartMode.WADB -> {
                if (adbPortHelper.tcpPort > 0) PreStartCheck.Success
                else if (!adbSettingsManager.hasWirelessDebugging) PreStartCheck.Failure.TlsNotSupported
                else null
            }
        }

    fun createStartSession(): RunnableSequence<StartStep> {
        val steps = mutableListOf<StartStep>()

        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                steps.add(StartStep.GetRootShell(::getRootShell))
            }

            StartMode.WADB -> {
                val tcpMode = preferencesRepository.tcpMode.get()
                val targetTcpPort = preferencesRepository.tcpPort.get()

                if (!tcpMode && tcpManager.isTcpPortOpen) {
                    steps.add(StartStep.CloseTcpPort(::closeTcpPort))
                }

                if (!adbSettingsManager.isUsbDebuggingEnabled) {
                    steps.add(StartStep.EnableUsbDebugging(adbSettingsManager::enableUsbDebugging))
                }

                if (!adbSettingsManager.isWirelessDebuggingEnabled &&
                    isWifiRequired &&
                    adbSettingsManager.hasWirelessDebugging
                ) {
                    steps.add(StartStep.EnableWirelessDebugging(::enableWirelessDebugging))
                }

                steps.add(StartStep.SearchForPort(::searchForPort))
                steps.add(StartStep.ConnectToPort(::connectToPort))

                if (tcpMode && !tcpManager.isTcpPortOpen(targetTcpPort)) {
                    steps.add(StartStep.OpenTcpPort(::openTcpPort))
                }
            }
        }

        steps.add(StartStep.ExecuteCommand(::executeCommand))
        steps.add(StartStep.WaitForService(::waitForService))

        return RunnableSequence(steps.toList())
    }

    suspend fun startService(sequence: RunnableSequence<StartStep>) {
        shizukuStateMachine.set(ShizukuStateMachine.State.STARTING)

        if (preferencesRepository.startMode.get() == StartMode.WADB) {
            adbSession = adbSessionFactory.create()
        }

        sequence.run()

        // Clean-up
        shizukuStateMachine.update()
        adbSession?.close()
        adbSession = null
    }

    fun stopService() {
        if (!shizukuStateMachine.isRunning()) return

        shizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
        runCatching { Shizuku.exit() }
    }

    // HELPER FUNCTIONS FOR START STEPS

    private suspend fun executeCommand() {
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                suspendCancellableCoroutine { cont ->
                    Shell.cmd(internalCommand).submit {
                        // TODO show log output
                        if (it.isSuccess) cont.resume(Unit)
                        else cont.resumeWithException(Exception("Failed to start with root"))
                    }
                }
            }

            StartMode.WADB -> {
                adbSession!!.withClient { client ->
                    client.command("shell:$internalCommand") { } // TODO show log output
                }
            }
        }
    }

    private suspend fun getRootShell() {
        withContext(Dispatchers.IO) {
            Shell.getShell {
                if (!it.isRoot) throw Exception("Device is not rooted")
            }
        }
    }

    @RequiresApi(30)
    private suspend fun enableWirelessDebugging(step: StartStep.EnableWirelessDebugging) {
        withContext(Dispatchers.IO) {
            adbSettingsManager.enableWirelessDebuggingAwaitingAuth { isAwaitingAuth ->
                step.updateAwaitingAuth(isAwaitingAuth)
            }
        }
    }

    private suspend fun searchForPort() {
        adbSession!!.port = adbPortHelper.getAdbPort(forceTls = isWifiRequired)
    }

    private suspend fun connectToPort() {
        adbSession!!.withClient { /* Connect only */ }
    }

    private suspend fun openTcpPort() {
        val targetPort = preferencesRepository.tcpPort.get()
        tcpManager.openTcpPort(targetPort, adbSession!!)
    }

    private suspend fun closeTcpPort() {
        tcpManager.closeTcpPort(adbSession!!)
    }

    private suspend fun waitForService() = withTimeout(60_000) {
        shizukuStateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }
    }
}
