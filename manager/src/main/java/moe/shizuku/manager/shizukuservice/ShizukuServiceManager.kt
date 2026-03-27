package moe.shizuku.manager.shizukuservice

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.R
import moe.shizuku.manager.core.adb.AdbClient
import moe.shizuku.manager.core.adb.AdbKey
import moe.shizuku.manager.core.adb.AdbKeyException
import moe.shizuku.manager.core.adb.AdbMdns
import moe.shizuku.manager.core.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.StartMode
import moe.shizuku.manager.core.extensions.hasWriteSecureSettings
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.core.utils.runnable.RunnableSequence
import moe.shizuku.manager.shizukuservice.models.NotRootedException
import moe.shizuku.manager.shizukuservice.models.StartStep
import moe.shizuku.manager.utils.ShizukuStateMachine
import java.io.EOFException
import java.io.File
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ShizukuServiceManager(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val environmentUtils: EnvironmentUtils,
    private val shizukuStateMachine: ShizukuStateMachine
) {

    private val starterFilePath by lazy {
        File(context.applicationInfo.nativeLibraryDir, "libshizuku.so").absolutePath
    }
    private val internalCommand by lazy {
        "$starterFilePath --apk=${context.applicationInfo.sourceDir}"
    }
    val adbCommand by lazy { "adb shell $starterFilePath" }

    private val _startSequence = MutableStateFlow<RunnableSequence<StartStep>?>(null)
    val startSequence = _startSequence.asStateFlow()

    private var currentAdbPort: Int = 0
    private var currentAdbKey: AdbKey? = null

    sealed class CanStartResult {
        object Success : CanStartResult()

        class Error {
            object NotRooted : CanStartResult()
            object TlsNotSupported : CanStartResult()
            object WriteSecureSettingsNotGranted : CanStartResult()
            object UsbDebuggingDisabled : CanStartResult()
            object WirelessDebuggingDisabled : CanStartResult()
        }
    }

    fun canStart(inBackground: Boolean = false): CanStartResult =
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                if (EnvironmentUtils.isRooted()) CanStartResult.Success
                else CanStartResult.Error.NotRooted
            }

            StartMode.WADB -> {
                val tcpPort = environmentUtils.getAdbTcpPort()
                if (tcpPort > 0) return CanStartResult.Success

                if (!environmentUtils.hasWirelessDebugging())
                    return CanStartResult.Error.TlsNotSupported

                if (inBackground) {
                    return if (context.hasWriteSecureSettings()) CanStartResult.Success
                    else CanStartResult.Error.WriteSecureSettingsNotGranted
                }

                val cr = context.contentResolver
                if (context.hasWriteSecureSettings()) {
                    Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                    Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

                    if (environmentUtils.isWifiRequired() && environmentUtils.hasWirelessDebugging()) {
                        Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                    }
                }

                val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
                val adbWifiEnabled = Settings.Global.getInt(cr, "adb_wifi_enabled", 0)

                return if (adbEnabled == 0) CanStartResult.Error.UsbDebuggingDisabled
                else if (adbWifiEnabled == 0) CanStartResult.Error.WirelessDebuggingDisabled
                else CanStartResult.Success
            }
        }

    suspend fun startService() {
        val steps = mutableListOf<StartStep>()

        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                steps.add(StartStep.RequestRoot(::requestRoot))
            }

            StartMode.WADB -> {
                val tcpMode = preferencesRepository.tcpMode.get()
                val targetTcpPort = preferencesRepository.tcpPort.get()
                val currentTcpPort = environmentUtils.getAdbTcpPort()

                if (currentTcpPort > 0 && !preferencesRepository.tcpMode.get()) {
                    steps.add(StartStep.CloseTcpPort(::closeTcpPort))
                }

                val cr = context.contentResolver
                val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) > 0
                val adbWifiEnabled = Settings.Global.getInt(cr, "adb_wifi_enabled", 0) > 0
                val wifiRequired = environmentUtils.isWifiRequired()
                val hasWirelessDebugging = environmentUtils.hasWirelessDebugging()

                if (!adbEnabled) {
                    steps.add(StartStep.EnableUsbDebugging(::enableUsbDebugging))
                }

                if (wifiRequired && hasWirelessDebugging && !adbWifiEnabled) {
                    steps.add(StartStep.EnableWirelessDebugging(::enableWirelessDebugging))
                }

                steps.add(StartStep.SearchForPort(::searchForPort))
                steps.add(StartStep.ConnectToPort(::connectToPort))

                if (tcpMode && currentTcpPort != targetTcpPort) {
                    steps.add(StartStep.OpenTcpPort(::openTcpPort))
                }
            }
        }

        steps.add(StartStep.ExecuteCommand(::executeCommand))
        steps.add(StartStep.WaitForService(::waitForService))

        val startSequence = RunnableSequence(steps.toList())
        _startSequence.value = startSequence
        shizukuStateMachine.set(ShizukuStateMachine.State.STARTING)
        startSequence.run()
    }

    private suspend fun requestRoot() {
        withContext(Dispatchers.IO) {
            if (!Shell.getShell().isRoot) {
                Shell.getCachedShell()?.close()
                throw NotRootedException()
            }
        }
    }

    private fun enableUsbDebugging() {
        val cr = context.contentResolver
        if (context.hasWriteSecureSettings()) {
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun enableWirelessDebugging() = callbackFlow {
        val cr = context.contentResolver
        var awaitingAuth = false
        var timeoutJob: Job? = null
        var unlockReceiver: BroadcastReceiver? = null

        fun finish() {
            trySend(Unit)
            close()
        }

        fun startTimeout() {
            timeoutJob?.cancel()
            timeoutJob = launch {
                delay(15_000)
                close(TimeoutException("Timed out during authorization"))
            }
        }

        fun handleAuth() {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (km.isKeyguardLocked) {
                val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
                unlockReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == Intent.ACTION_USER_PRESENT) {
                            context.unregisterReceiver(this)
                            unlockReceiver = null
                            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                        }
                    }
                }
                context.registerReceiver(unlockReceiver, filter)
            } else {
                awaitingAuth = true
            }
            timeoutJob?.cancel()
        }

        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                when (Settings.Global.getInt(cr, "adb_wifi_enabled", 0)) {
                    0 -> {
                        if (awaitingAuth) {
                            close(SecurityException("Network is not authorized for wireless debugging"))
                        } else {
                            handleAuth()
                        }
                    }
                    1 -> finish()
                }
            }
        }

        cr.registerContentObserver(
            Settings.Global.getUriFor("adb_wifi_enabled"), false, observer
        )

        if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1) {
            finish()
        } else {
            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
            startTimeout()
        }

        awaitClose {
            timeoutJob?.cancel()
            cr.unregisterContentObserver(observer)
            unlockReceiver?.let { context.unregisterReceiver(it) }
        }
    }.first()

    private suspend fun searchForPort() {
        currentAdbPort = environmentUtils.getAdbTcpPort()
            .takeUnless { it <= 0 || environmentUtils.isWifiRequired() } ?:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) discoverPort()
                else throw IllegalStateException("ADB port not found")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun discoverPort(): Int = suspendCancellableCoroutine { cont ->
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

    private suspend fun connectToPort() {
        withContext(Dispatchers.IO) {
            currentAdbKey = runCatching {
                AdbKey(PreferenceAdbKeyStore(preferencesRepository.prefs), "shizuku")
            }.getOrElse {
                if (it is CancellationException) throw it
                else throw AdbKeyException(it)
            }

            if (currentAdbPort <= 0) {
                currentAdbPort = environmentUtils.getAdbTcpPort()
            }

            if (currentAdbPort <= 0) {
                throw IllegalStateException("ADB port not found")
            }

            // Just test connection
            AdbClient("127.0.0.1", currentAdbPort, currentAdbKey!!).use { client ->
                connectWithRetry(client)
            }
        }
    }

    private suspend fun openTcpPort() {
        val tcpPort = preferencesRepository.tcpPort.get()
        if (currentAdbPort != tcpPort) {
            withContext(Dispatchers.IO) {
                AdbClient("127.0.0.1", currentAdbPort, currentAdbKey!!).use { client ->
                    connectWithRetry(client)
                    runCatching {
                        client.command("tcpip:$tcpPort")
                    }.onFailure { if (it !is EOFException && it !is SocketException) throw it }
                }
            }
            currentAdbPort = tcpPort
        }
    }

    private suspend fun executeCommand() {
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                suspendCancellableCoroutine { cont ->
                    Shell.cmd(internalCommand).to(object : CallbackList<String?>() {
                        override fun onAddElement(s: String?) {
                            s?.let { TODO() }
                        }
                    }).submit {
                        if (it.isSuccess) cont.resume(Unit)
                        else {
                            shizukuStateMachine.update()
                            cont.resumeWithException(Exception("Failed to start with root"))
                        }
                    }
                }
            }

            StartMode.WADB -> {
                withContext(Dispatchers.IO) {
                    AdbClient("127.0.0.1", currentAdbPort, currentAdbKey!!).use { client ->
                        connectWithRetry(client)
                        client.command("shell:$internalCommand") { TODO() }
                    }
                }
            }
        }
    }

    private suspend fun waitForService() {
        try {
            withTimeout(60_000) {
                shizukuStateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }
            }
        } catch (_: TimeoutCancellationException) {
            throw TimeoutException("Failed to receive binder within 1 minute")
        }
    }

    suspend fun closeTcpPort() {
        runCatching {
            val cr = context.contentResolver
            if (context.hasWriteSecureSettings()) {
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
            }

            val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
            if (adbEnabled == 0) throw IllegalStateException("ADB is not enabled")

            shizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
            val key = AdbKey(PreferenceAdbKeyStore(preferencesRepository.prefs), "shizuku")
            withContext(Dispatchers.IO) {
                AdbClient("127.0.0.1", environmentUtils.getAdbTcpPort(), key).use { client ->
                    connectWithRetry(client)
                    client.command("usb:")
                }
            }
        }.onFailure {
            if (environmentUtils.getAdbTcpPort() > 0) {
                shizukuStateMachine.update()
                withContext(Dispatchers.Main) {
                    context.toast(R.string.tcp_error_closing)
                }
            }
        }
    }

    private suspend fun connectWithRetry(client: AdbClient) {
        var delayTime = 0L
        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            try {
                delay(delayTime)
                client.connect()
                break
            } catch (e: Exception) {
                if (attempt == maxAttempts || e is CancellationException || e is SocketTimeoutException) {
                    throw e
                }
                delayTime += 1000
            }
        }
    }
}
