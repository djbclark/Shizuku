package moe.shizuku.manager.intents.data

import kotlinx.coroutines.flow.map
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.string

class TokenRepository(
    preferencesRepository: PreferencesRepository
) {
    private val authToken by preferencesRepository.pref { string("auth_token", null) }

    val authTokenFlow = authToken.flow.map {
        it ?: regenerateAuthToken()
    }

    fun getAuthToken() =
        authToken.get().takeUnless { it.isNullOrEmpty() }
            ?: regenerateAuthToken()

    fun regenerateAuthToken() =
        generateToken().also { authToken.set(it) }
}
