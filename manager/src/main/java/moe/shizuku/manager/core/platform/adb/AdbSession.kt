package moe.shizuku.manager.core.platform.adb

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.shizuku.manager.core.extensions.resultOf
import moe.shizuku.manager.core.platform.adb.client.AdbClient
import moe.shizuku.manager.core.platform.adb.client.AdbKey
import moe.shizuku.manager.core.platform.adb.client.PreferenceAdbKeyStore
import moe.shizuku.manager.core.platform.adb.models.AdbConnectionError
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import java.io.IOException
import javax.net.ssl.SSLProtocolException

class AdbSession(
    private val preferencesRepository: PreferencesRepository,
    initialPort: Int
) : AutoCloseable {
    private val mutex = Mutex()
    private var key: AdbKey? = null
    private var client: AdbClient? = null
    private var isConnected = false

    private suspend fun getKey(): AdbKey = key ?: withContext(Dispatchers.IO) {
        AdbKey(PreferenceAdbKeyStore(preferencesRepository.prefs), "shizuku").also { key = it }
    }

    var port: Int = initialPort
        set(value) {
            if (field != value) {
                field = value
                close()
            }
        }

    suspend fun connect(): Result<AdbClient, AdbConnectionError> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isConnected) return@withLock Ok(client!!)

            require(port > 0) { "Port must be greater than 0" }
            val client = client ?: AdbClient("127.0.0.1", port, getKey()).also {
                this@AdbSession.client = it
            }

            connectWithRetry(client).onOk { isConnected = true }
        }
    }

    private suspend fun connectWithRetry(client: AdbClient): Result<AdbClient, AdbConnectionError> {
        val maxAttempts = 5
        repeat(maxAttempts) { attempt ->
            if (attempt > 0) delay(1000L * attempt)

            resultOf { client.connect() }
                .fold(
                    success = { return Ok(client) },
                    failure = {
                        when (it) {
                            is SSLProtocolException -> return Err(AdbConnectionError.NotPaired)
                            is IOException -> {
                                val lastAttempt = maxAttempts - 1
                                if (attempt == lastAttempt)
                                    return Err(AdbConnectionError.ConnectionFailed(it))
                            }

                            else -> throw it
                        }
                    }
                )
        }
        error("Unexpected retry exit")
    }

    suspend fun <T> withClient(block: suspend (AdbClient) -> T): Result<T, AdbConnectionError> =
        withContext(Dispatchers.IO) {
            connect().andThen { client ->
                resultOf { block(client) }
                    .mapError {
                        if (it is IOException) {
                            close()
                            AdbConnectionError.ConnectionFailed(it)
                        } else throw it
                    }
            }
        }

    override fun close() {
        client?.close()
        client = null
        isConnected = false
    }

    class Factory(private val preferencesRepository: PreferencesRepository) {
        fun create(port: Int = 0) = AdbSession(preferencesRepository, port)
    }
}
