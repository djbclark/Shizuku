package moe.shizuku.manager.core.platform.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.platform.adb.client.AdbClient
import moe.shizuku.manager.core.platform.adb.client.AdbKey
import moe.shizuku.manager.core.platform.adb.client.AdbKeyException
import moe.shizuku.manager.core.platform.adb.client.PreferenceAdbKeyStore
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import java.io.EOFException
import java.net.SocketTimeoutException

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

        val port = this.port
        check (port > 0) { "Port must be greater than 0" }

        val newClient = AdbClient("127.0.0.1", port, getKey())
        newClient.connectWithRetry()
        _client = newClient
        return newClient
    }

    private suspend fun AdbClient.connectWithRetry() {
        var delayTime = 0L
        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            if (delayTime > 0) delay(delayTime)

            resultOf { connect() }
                .onSuccess { return }
                .onFailure {
                    if (attempt >= maxAttempts) throw it

                    if (it is EOFException || it is SocketTimeoutException) {
                        delayTime += 1000
                        continue
                    } else throw it
                }
        }
    }

    private fun closeClient() {
        val client = _client
        _client = null
        client?.close()
    }

    override fun close(): Unit = closeClient()

    class Factory(
        private val preferencesRepository: PreferencesRepository
    ) {
        fun create(port: Int = 0): AdbSession =
            AdbSession(preferencesRepository, port)
    }
}