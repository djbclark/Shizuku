package moe.shizuku.manager.intents.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.preferences.data.string

class TokenRepository(
    preferencesRepository: PreferencesRepository
) {
    private val authToken by preferencesRepository.pref { string("auth_token") }

    val authTokenFlow: Flow<String> = authToken.flow.map {
        it ?: regenerateAuthToken()
    }

    fun getAuthToken(): String =
        authToken.get().takeUnless { it.isNullOrEmpty() }
            ?: regenerateAuthToken()

    fun regenerateAuthToken(): String =
        generateToken().also { authToken.set(it) }
}
