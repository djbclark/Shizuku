package moe.shizuku.manager.intents.data

import moe.shizuku.manager.core.data.preferences.PreferencesRepository.pref
import moe.shizuku.manager.core.data.preferences.string

object TokenRepository {
    private val authToken by pref { string("auth_token", null) }

    fun getAuthToken(): String =
        authToken.value.takeUnless { it.isNullOrEmpty() }
            ?: regenerateAuthToken()

    fun regenerateAuthToken(): String =
        generateToken().also { authToken.value = it }
}
