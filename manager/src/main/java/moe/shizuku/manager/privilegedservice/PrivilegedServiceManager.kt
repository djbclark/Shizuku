package moe.shizuku.manager.privilegedservice

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.R
import moe.shizuku.manager.core.adb.AdbManager
import moe.shizuku.manager.core.adb.AdbSession
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.core.utils.runnable.RunnableSequence
import moe.shizuku.manager.core.utils.runnable.RunnableStatus
import moe.shizuku.manager.privilegedservice.models.NotRootedException
import moe.shizuku.manager.privilegedservice.models.StartStep
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
class PrivilegedServiceManager(
    private val context: Context,
    private val adbManager: AdbManager,
    private val tcpModeManager: TcpModeManager,
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
    val adbCommand by lazy { "adb shell $starterFilePath" }

    private val _activeSequence = MutableStateFlow<RunnableSequence<StartStep>?>(null)

    val startSteps: Flow<List<StartStep>> = _activeSequence.flatMapLatest {
        it?.steps ?: flowOf(emptyList())
    }

    val startStatus: Flow<RunnableStatus> = _activeSequence.flatMapLatest {
        it?.status ?: flowOf(RunnableStatus.Pending)
    }

    private var activeSession: AdbSession? = null

    sealed class CanStartResult {
        object Success : CanStartResult()

        sealed class Error(@param:StringRes val msgRes: Int) : CanStartResult() {
            object NotRooted :
                Error(R.string.start_error_root)

            object TlsNotSupported :
                Error(R.string.start_error_tls_not_supported)

            object WriteSecureSettingsNotGranted :
                Error(R.string.start_error_write_secure_settings)

            object UsbDebuggingDisabled :
                Error(R.string.start_error_usb_debugging_disabled)

            object WirelessDebuggingDisabled :
                Error(R.string.start_error_wireless_debugging_disabled)

            object WifiRequired :
                Error(R.string.start_error_wifi_required)

            object AuthorizationRequired :
                Error(R.string.start_error_authorization_required)
        }
    }

    val isWifiRequired: Boolean
        get() = preferencesRepository.startMode.get() == StartMode.WADB &&
                (adbManager.getTcpPort() <= 0 || !preferencesRepository.tcpMode.get())

    fun canStart(inBackground: Boolean = false): CanStartResult =
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                if (EnvironmentUtils.isRooted()) CanStartResult.Success
                else CanStartResult.Error.NotRooted
            }

            StartMode.WADB -> {
                if (adbManager.getTcpPort() > 0) return CanStartResult.Success

                if (!adbManager.hasWirelessDebugging) return CanStartResult.Error.TlsNotSupported

                if (inBackground) {
                    return if (context.hasWriteSecureSettings()) CanStartResult.Success
                    else CanStartResult.Error.WriteSecureSettingsNotGranted
                }

                runCatching { adbManager.enableUsbDebugging() }
                    .getOrElse { CanStartResult.Error.UsbDebuggingDisabled }

                runCatching { adbManager.enableWirelessDebugging() }
                    .fold(
                        onSuccess = { enabled ->
                            if (enabled) CanStartResult.Success
                            else CanStartResult.Error.AuthorizationRequired
                        },
                        onFailure = {
                            if (it is IllegalStateException) CanStartResult.Error.WifiRequired
                            else CanStartResult.Error.WirelessDebuggingDisabled
                        }
                    )
            }
        }.also { Log.i(TAG, "canStartResult: ${it::class.simpleName}") }

    suspend fun startService() {
        shizukuStateMachine.set(ShizukuStateMachine.State.STARTING)

        if (preferencesRepository.startMode.get() == StartMode.WADB) {
            adbSessionFactory.create().also {
                activeSession = it
            }
        }

        try {
            val sequence = buildStartSequence()
            _activeSequence.value = sequence
            sequence.run()
        } finally {
            shizukuStateMachine.update()
            activeSession?.close()
            activeSession = null
            _activeSequence.value = null
        }
    }

    fun stopService() {
        if (!shizukuStateMachine.isRunning()) return

        shizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
        runCatching { Shizuku.exit() }
    }

    private fun buildStartSequence(): RunnableSequence<StartStep> {
        val steps = mutableListOf<StartStep>()

        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                steps.add(StartStep.RequestRoot(::requestRoot))
            }

            StartMode.WADB -> {
                val tcpMode = preferencesRepository.tcpMode.get()
                val targetTcpPort = preferencesRepository.tcpPort.get()

                if (!tcpMode && tcpModeManager.isTcpPortOpen) {
                    steps.add(StartStep.CloseTcpPort(::closeTcpPort))
                }

                if (!adbManager.isUsbDebuggingEnabled) {
                    steps.add(StartStep.EnableUsbDebugging(adbManager::enableUsbDebugging))
                }

                if (!adbManager.isWirelessDebuggingEnabled &&
                    isWifiRequired &&
                    adbManager.hasWirelessDebugging
                ) {
                    steps.add(StartStep.EnableWirelessDebugging(adbManager::enableWirelessDebuggingAwaitingAuth))
                }

                steps.add(StartStep.SearchForPort(::searchForPort))
                steps.add(StartStep.ConnectToPort(::connectToPort))

                if (tcpMode && !tcpModeManager.isTcpPortOpen(targetTcpPort)) {
                    steps.add(StartStep.OpenTcpPort(::openTcpPort))
                }
            }
        }

        steps.add(StartStep.ExecuteCommand(::executeCommand))
        steps.add(StartStep.WaitForService(::waitForService))

        return RunnableSequence(steps.toList())
    }

    private suspend fun executeCommand() {
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                suspendCancellableCoroutine { cont ->
                    Shell.cmd(internalCommand).to(object : CallbackList<String?>() {
                        override fun onAddElement(s: String?) {
                            s?.let { } // TODO
                        }
                    }).submit {
                        if (it.isSuccess) cont.resume(Unit)
                        else cont.resumeWithException(Exception("Failed to start with root"))
                    }
                }
            }

            StartMode.WADB -> {
                activeSession!!.withClient { client ->
                    client.command("shell:$internalCommand") { } // TODO
                }
            }
        }
    }

    private suspend fun requestRoot() = withContext(Dispatchers.IO) {
        if (!EnvironmentUtils.isRooted()) {
            Shell.getCachedShell()?.close()
            throw NotRootedException()
        }
    }

    private suspend fun searchForPort() {
        activeSession!!.port = adbManager.getAdbPort(forceTls = isWifiRequired)
    }

    private suspend fun connectToPort() {
        activeSession!!.withClient { /* Connect only */ }
    }

    private suspend fun openTcpPort() {
        val targetPort = preferencesRepository.tcpPort.get()
        tcpModeManager.openTcpPort(targetPort, activeSession!!)
    }

    private suspend fun closeTcpPort() {
        tcpModeManager.closeTcpPort(activeSession!!)
    }

    private suspend fun waitForService() = withTimeout(60_000) {
        shizukuStateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }
    }
}
