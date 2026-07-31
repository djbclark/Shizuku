package moe.shizuku.manager.adb

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.pm.PackageManager
import android.content.Context
import android.provider.Settings
import android.util.Log
import moe.shizuku.manager.utils.HeadlessLogger
import android.widget.Toast
import java.io.EOFException
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.ShizukuStateMachine

object AdbStarter {
    private val directScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startDirect(context: Context, port: Int, maxRetries: Int = 3, retryDelayMs: Long = 5000) {
        if (ShizukuStateMachine.get() == ShizukuStateMachine.State.STARTING) {
            Log.w(AppConstants.TAG, "startDirect: already starting, ignoring duplicate request")
            return
        }
        directScope.launch {
            var lastError: Exception? = null
            for (attempt in 1..maxRetries) {
                try {
                    startAdb(context, port)
                    return@launch
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    lastError = e
                    if (attempt < maxRetries) {
                        Log.w(AppConstants.TAG, "startAdb attempt $attempt/$maxRetries failed, retrying in ${retryDelayMs}ms", e)
                        delay(retryDelayMs)
                    }
                }
            }
            Log.w(AppConstants.TAG, "Direct ADB start failed after $maxRetries attempts", lastError)
            // startAdb() sets STARTING on entry but never resets on total failure,
            // wedging every future start attempt behind the "already starting"
            // guard above until something else (e.g. force-stop) clears it.
            // update() reconciles against the real binder state (STOPPED unless
            // a retry happened to succeed underneath us).
            ShizukuStateMachine.update()
        }
    }

    suspend fun startAdb(context: Context, port: Int, log: ((String) -> Unit)? = null) {
        suspend fun AdbClient.runCommand(cmd: String) {
            command(cmd) { log?.invoke(String(it)) }
        }

        try {
            ShizukuStateMachine.set(ShizukuStateMachine.State.STARTING)
            log?.invoke("Starting with wireless adb...\n")
        
            withContext(Dispatchers.IO) {
                val key = runCatching { AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku") }
                    .getOrElse {
                        if (it is CancellationException) throw it
                        else throw AdbKeyException(it)
                    }

                var activePort = port
                val tcpMode = ShizukuSettings.getTcpMode()
                val tcpPort = ShizukuSettings.getTcpPort()
                var justSwitchedToTcpMode = false
                if (tcpMode && activePort != tcpPort) {
                    log?.invoke("Connecting on port $activePort...")

                    AdbClient("127.0.0.1", activePort, key).use { client ->
                        client.connect()

                        log?.invoke("Successfully connected on port $activePort...")
                        log?.invoke("\nRestarting in TCP mode port: $tcpPort")

                        activePort = tcpPort
                        runCatching {
                            client.command("tcpip:$activePort")
                        }.onFailure { if (it !is EOFException && it !is SocketException) throw it } // Expected when ADB restarts in TCP mode
                    }
                    justSwitchedToTcpMode = true
                }

                log?.invoke("Connecting on port $activePort...")

                AdbClient("127.0.0.1", activePort, key).use { client ->
                    try {
                        connectWithRetry(client, maxAttempts = RECONNECT_MAX_ATTEMPTS)
                    } catch (e: EOFException) {
                        // Only the reconnect immediately after switching to TCP mode is expected
                        // to need this wider, friendlier-messaged retry window (adbd restarting
                        // takes a moment) — an EOF on an ordinary connect (no preceding tcpip:
                        // switch) is a genuine, unrelated connection failure and should surface as
                        // the normal connection-error path instead of claiming "still restarting."
                        if (justSwitchedToTcpMode) throw PostTcpipReconnectException(e) else throw e
                    }
                    log?.invoke("Successfully connected on port $activePort...\n")
                    client.runCommand("shell:${Starter.internalCommand}")
                }
            }
        } finally {
            if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
                Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 0)
        }
    }

    suspend fun stopTcp(context: Context, port: Int) {
        runCatching {
            val cr = context.contentResolver
            if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
            }
        
            val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
            if (adbEnabled == 0) throw IllegalStateException("ADB is not enabled")

            ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
            val key = AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
            withContext(Dispatchers.IO) {
                AdbClient("127.0.0.1", port, key).use { client ->
                    connectWithRetry(client)
                    client.command("usb:")
                }
            }
        }.onFailure {
            if (EnvironmentUtils.getAdbTcpPort() > 0) {
                ShizukuStateMachine.update()
                withContext(Dispatchers.Main) {
                    val errorMsg = when (it) {
                        is AdbKeyException -> context.getString(R.string.adb_error_key_store)
                        else -> it.message
                    }
                    Toast.makeText(context, context.getString(R.string.adb_error_stop_tcp) + ". ${errorMsg}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    // 5 attempts (0+1+2+3+4=10s total) wasn't enough window on some devices for adbd to finish
    // restarting into TCP mode before the reconnect gave up with a raw EOFException (confirmed
    // live against issue #43 on hd8 — a device that's generally slow). Widen only the post-tcpip
    // reconnect in startAdb() to 10 attempts (~45s); leave stopTcp()'s connect at the original
    // budget via the default so "stop wireless debugging" doesn't hang longer waiting on a
    // genuinely-unreachable client.
    private const val RECONNECT_MAX_ATTEMPTS = 10
    private const val DEFAULT_MAX_ATTEMPTS = 5

    private suspend fun connectWithRetry(client: AdbClient, maxAttempts: Int = DEFAULT_MAX_ATTEMPTS) {
        var delayTime = 0L
        for (attempt in 1..maxAttempts) {
            try {
                delay(delayTime)
                client.connect()
                break
            } catch (e: Exception) {
                if (
                    attempt == maxAttempts ||
                    e is CancellationException ||
                    e is SocketTimeoutException
                ) throw e
                delayTime += 1000
            }
        }
    }
}