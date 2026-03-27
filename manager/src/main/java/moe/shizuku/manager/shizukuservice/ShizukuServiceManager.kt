package moe.shizuku.manager.shizukuservice

import android.app.KeyguardManager
import android.app.Notification
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
import kotlinx.coroutines.flow.StateFlow
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

    private val starterFile = File(context.applicationInfo.nativeLibraryDir, "libshizuku.so")

    val userCommand: String = starterFile.absolutePath
    val adbCommand = "adb shell $userCommand"
    val internalCommand = "$userCommand --apk=${context.applicationInfo.sourceDir}"

    val serviceStartedMessage =
        "Service started, this window will be automatically closed in 3 seconds"

    private val _startProcess = MutableStateFlow<ServiceStartProcess?>(null)
    val startProcess: StateFlow<ServiceStartProcess?> = _startProcess.asStateFlow()

    private var currentAdbPort: Int = 0
    private var currentAdbKey: AdbKey? = null
    private var logCallback: ((String) -> Unit)? = null
    private var onShowForegroundCallback: (suspend (Notification) -> Unit)? = null
    private var buildForegroundNotificationCallback: (() -> Notification)? = null

    sealed class CanStartResult {
        object Success : CanStartResult()

        class Error {
            object NotRooted : CanStartResult()
            object UsbDebuggingDisabled : CanStartResult()
            object WirelessDebuggingDisabled : CanStartResult()
            object TlsNotSupported : CanStartResult()
        }
    }

    fun canStart(): CanStartResult =
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                if (EnvironmentUtils.isRooted()) CanStartResult.Success
                else CanStartResult.Error.NotRooted
            }

            StartMode.WADB -> {
                val cr = context.contentResolver
                if (context.hasWriteSecureSettings()) {
                    Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                    Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

                    if (environmentUtils.isWifiRequired() && environmentUtils.isTlsSupported()) {
                        Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                    }
                }

                val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
                val adbWifiEnabled = Settings.Global.getInt(cr, "adb_wifi_enabled", 0)
                val tcpPort = environmentUtils.getAdbTcpPort()

                return if (adbEnabled == 0)
                    CanStartResult.Error.UsbDebuggingDisabled
                else if (tcpPort <= 0 && !environmentUtils.isTlsSupported())
                    CanStartResult.Error.TlsNotSupported
                else if (tcpPort <= 0 && adbWifiEnabled == 0)
                    CanStartResult.Error.WirelessDebuggingDisabled
                else CanStartResult.Success
            }
        }

    suspend fun startService(
        log: ((String) -> Unit)? = null,
        onShowForeground: (suspend (Notification) -> Unit)? = null,
        buildForegroundNotification: (() -> Notification)? = null
    ) {
        logCallback = log
        onShowForegroundCallback = onShowForeground
        buildForegroundNotificationCallback = buildForegroundNotification

        val builder = ServiceStartProcess.Builder { runStep(it) }

        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                builder.addStep(StartStep.RequestingRoot)
                builder.addStep(StartStep.ExecutingCommand)
            }

            StartMode.WADB -> {
                val tcpPort = environmentUtils.getAdbTcpPort()
                if (tcpPort > 0 && !preferencesRepository.tcpMode.get()) {
                    builder.addStep(StartStep.ClosingTcpPort)
                }

                builder.addStep(StartStep.SearchingForPort)

                if (onShowForeground != null && buildForegroundNotification != null) {
                    builder.addStep(StartStep.AwaitingAuthorization)
                }

                builder.addStep(StartStep.ConnectingToPort)

                if (preferencesRepository.tcpMode.get()) {
                    builder.addStep(StartStep.OpeningTcpPort)
                }

                builder.addStep(StartStep.ExecutingCommand)
            }
        }
        builder.addStep(StartStep.WaitingForService)

        val process = builder.build()
        _startProcess.value = process
        process.run()
    }

    private suspend fun runStep(step: StartStep) {
        when (step) {
            StartStep.RequestingRoot -> stepRequestingRoot()
            StartStep.ClosingTcpPort -> stepClosingTcpPort()
            StartStep.SearchingForPort -> stepSearchingForPort()
            StartStep.AwaitingAuthorization -> stepAwaitingAuthorization()
            StartStep.ConnectingToPort -> stepConnectingToPort()
            StartStep.OpeningTcpPort -> stepOpeningTcpPort()
            StartStep.ExecutingCommand -> stepExecutingCommand()
            StartStep.WaitingForService -> stepWaitingForService()
        }
    }

    private suspend fun stepRequestingRoot() {
        withContext(Dispatchers.IO) {
            if (!Shell.getShell().isRoot) {
                Shell.getCachedShell()?.close()
                throw NotRootedException()
            }
        }
    }

    private suspend fun stepClosingTcpPort() {
        stopTcp()
    }

    private suspend fun stepSearchingForPort() {
        if (environmentUtils.isWifiRequired()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            logCallback?.invoke("Searching for wireless debugging service...")
            currentAdbPort = if (onShowForegroundCallback != null && buildForegroundNotificationCallback != null) {
                // If we have foreground callbacks, we might need auth, but this step only does discovery
                // However, the original logic combined them. Let's split if possible or just handle here.
                discoverPort(context)
            } else {
                discoverPort(context)
            }
            logCallback?.invoke("Found port: $currentAdbPort")
        } else {
            currentAdbPort = environmentUtils.getAdbTcpPort()
        }
        }
    }

    private suspend fun stepAwaitingAuthorization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val onShow = onShowForegroundCallback ?: return
            val buildNotif = buildForegroundNotificationCallback ?: return
            currentAdbPort = discoverPortWithAuth(onShow, buildNotif)
            logCallback?.invoke("Authorized and found port: $currentAdbPort")
        }
    }

    private suspend fun stepConnectingToPort() {
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

            logCallback?.invoke("Connecting to port $currentAdbPort...")
            // Just test connection
            AdbClient("127.0.0.1", currentAdbPort, currentAdbKey!!).use { client ->
                connectWithRetry(client)
                logCallback?.invoke("Successfully connected to port $currentAdbPort")
            }
        }
    }

    private suspend fun stepOpeningTcpPort() {
        val tcpPort = preferencesRepository.tcpPort.get()
        if (currentAdbPort != tcpPort) {
            logCallback?.invoke("\nRestarting in TCP mode port: $tcpPort")
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

    private suspend fun stepExecutingCommand() {
        shizukuStateMachine.set(ShizukuStateMachine.State.STARTING)
        when (preferencesRepository.startMode.get()) {
            StartMode.ROOT -> {
                suspendCancellableCoroutine { cont ->
                    Shell.cmd(internalCommand)
                        .to(object : CallbackList<String?>() {
                            override fun onAddElement(s: String?) {
                                s?.let { logCallback?.invoke(it) }
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
                        client.command("shell:$internalCommand") { logCallback?.invoke(String(it)) }
                    }
                }
            }
        }
    }

    private suspend fun stepWaitingForService() {
        waitForBinder(logCallback)
    }

    suspend fun waitForBinder(log: ((String) -> Unit)? = null) {
        try {
            log?.invoke("\nWaiting for service. This may take up to 1 minute...")
            withTimeout(60_000) {
                shizukuStateMachine.asFlow()
                    .first { it == ShizukuStateMachine.State.RUNNING }
            }
            log?.invoke(serviceStartedMessage)
        } catch (e: TimeoutCancellationException) {
            throw TimeoutException("Failed to receive binder within 1 minute")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun discoverPort(context: Context): Int = suspendCancellableCoroutine { cont ->
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

    suspend fun discoverPortWithAuth(
        onShowForeground: suspend (Notification) -> Unit,
        buildForegroundNotification: () -> Notification
    ): Int = callbackFlow {
        val cr = context.contentResolver
        val adbMdns = AdbMdns(context, AdbMdns.TLS_CONNECT) { p ->
            if (p > 0) trySend(p)
        }

        var awaitingAuth = false
        var timeoutJob: Job? = null
        var unlockReceiver: BroadcastReceiver? = null

        fun startDiscoveryWithTimeout() {
            adbMdns.start()
            timeoutJob?.cancel()
            timeoutJob = launch {
                delay(15_000)
                close(TimeoutException("Timed out during mDNS port discovery"))
            }
        }

        fun handleAuth() {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (km.isKeyguardLocked) {
                launch { onShowForeground(buildForegroundNotification()) }

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
            adbMdns.stop()
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

                    1 -> startDiscoveryWithTimeout()
                }
            }
        }

        Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
        cr.registerContentObserver(
            Settings.Global.getUriFor("adb_wifi_enabled"),
            false,
            observer
        )
        startDiscoveryWithTimeout()

        awaitClose {
            adbMdns.stop()
            timeoutJob?.cancel()
            cr.unregisterContentObserver(observer)
            unlockReceiver?.let { context.unregisterReceiver(it) }
        }
    }.first()

    suspend fun stopTcp() {
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
