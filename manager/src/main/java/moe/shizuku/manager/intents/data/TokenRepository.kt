package moe.shizuku.manager.intents.data

import moe.shizuku.manager.core.data.KeyValueDataSource
import moe.shizuku.manager.core.data.KeyValueEntry

private val AUTH_TOKEN =
    KeyValueEntry<String?>(
        key = "auth_token",
        default = null,
    )

object TokenRepository {
    val source = KeyValueDataSource

    fun getAuthToken() =
        source.get(AUTH_TOKEN).takeIf { !it.isNullOrEmpty() }
            ?: regenerateAuthToken()

    fun regenerateAuthToken() =
        generateToken().also { setAuthToken(it) }

    private fun setAuthToken(value: String?) =
        source.set(AUTH_TOKEN, value)
}