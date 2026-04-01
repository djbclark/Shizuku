package moe.shizuku.manager.core.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.adb.client.AdbClient
import moe.shizuku.manager.core.adb.client.AdbKey
import moe.shizuku.manager.core.adb.client.AdbKeyException
import moe.shizuku.manager.core.adb.client.PreferenceAdbKeyStore
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

class AdbSession(
    private val preferencesRepository: PreferencesRepository,
    port: Int
) : AutoCloseable {
    private var key: AdbKey? = null

    private suspend fun getKey() = key ?: withContext(Dispatchers.IO) {
        runCatching {
            AdbKey(
                PreferenceAdbKeyStore(preferencesRepository.prefs),
                "shizuku"
            )
        }.getOrElse { throw AdbKeyException(it) }.also { key = it }
    }

    var port: Int = port
        set(value) {
            if (field != value) {
                field = value
                closeClient()
            }
        }

    private var _client: AdbClient? = null

    suspend fun <T> withClient(block: suspend (AdbClient) -> T): T = withContext(Dispatchers.IO) {
        block(getClient())
    }

    private suspend fun getClient(): AdbClient {
        val currentClient = _client
        if (currentClient != null) return currentClient

        if (port <= 0) throw IllegalStateException("Port not set")

        val newClient = AdbClient("127.0.0.1", port, getKey())
        newClient.connectWithRetry()
        _client = newClient
        return newClient
    }

    private suspend fun AdbClient.connectWithRetry() {
        var delayTime = 0L
        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            try {
                if (delayTime > 0) delay(delayTime)
                connect()
                return
            } catch (e: Exception) {
                if (attempt == maxAttempts ||
                    e is CancellationException ||
                    e is SocketTimeoutException
                ) {
                    throw e
                }
                delayTime += 1000
            }
        }
    }

    private fun closeClient() {
        val client = _client
        _client = null
        client?.close()
    }

    override fun close() = closeClient()

    class Factory(
        private val preferencesRepository: PreferencesRepository
    ) {
        fun create(port: Int = 0) =
            AdbSession(preferencesRepository, port)
    }
}