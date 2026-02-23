package moe.shizuku.manager.core.data.preferences

import moe.shizuku.manager.core.models.preferences.IntEnum
import moe.shizuku.manager.core.models.preferences.StartMode
import moe.shizuku.manager.core.models.preferences.Theme
import moe.shizuku.manager.core.models.preferences.UpdateChannel

object PreferencesRepository {
    private val prefs = PreferencesDataSource

    // -------------------------
    // GETTERS
    // -------------------------

    fun getStartMode(): StartMode = getEnum(Preferences.START_MODE)

    fun getStartOnBoot(): Boolean = prefs.get(Preferences.START_ON_BOOT)

    fun getWatchdog(): Boolean = prefs.get(Preferences.WATCHDOG)

    fun getTcpMode(): Boolean = prefs.get(Preferences.TCP_MODE)

    fun getTcpPort(): Int = prefs.get(Preferences.TCP_PORT)

    fun getAutoDisableUsbDebugging(): Boolean = prefs.get(Preferences.AUTO_DISABLE_USB_DEBUGGING)

    fun getLanguage(): String? = prefs.get(Preferences.LANGUAGE)

    fun getTheme(): Theme = getEnum(Preferences.THEME)

    fun getAmoledBlack(): Boolean = prefs.get(Preferences.AMOLED_BLACK)

    fun getDynamicColor(): Boolean = prefs.get(Preferences.DYNAMIC_COLOR)

    fun getCheckForUpdates(): Boolean = prefs.get(Preferences.CHECK_FOR_UPDATES)

    fun getUpdateChannel(): UpdateChannel = getEnum(Preferences.UPDATE_CHANNEL)

    fun getLastPromptedVersion(): String? = prefs.get(Preferences.LAST_PROMPTED_VERSION)

    fun getLegacyPairing(): Boolean = prefs.get(Preferences.LEGACY_PAIRING)

    fun getAuthToken(): String? = prefs.get(Preferences.AUTH_TOKEN)

    // -------------------------
    // SETTERS
    // -------------------------

    fun setStartMode(value: StartMode) =
        setEnum(
            Preferences.START_MODE,
            value,
        )

    fun setStartOnBoot(value: Boolean) =
        prefs.set(
            Preferences.START_ON_BOOT,
            value,
        )

    fun setWatchdog(value: Boolean) =
        prefs.set(
            Preferences.WATCHDOG,
            value,
        )

    fun setTcpMode(value: Boolean) =
        prefs.set(
            Preferences.TCP_MODE,
            value,
        )

    fun setTcpPort(value: Int) =
        prefs.set(
            Preferences.TCP_PORT,
            value,
        )

    fun setAutoDisableUsbDebugging(value: Boolean) =
        prefs.set(
            Preferences.AUTO_DISABLE_USB_DEBUGGING,
            value,
        )

    fun setLanguage(value: String?) =
        prefs.set(
            Preferences.LANGUAGE,
            value,
        )

    fun setTheme(value: Theme) =
        setEnum(
            Preferences.THEME,
            value,
        )

    fun setAmoledBlack(value: Boolean) =
        prefs.set(
            Preferences.AMOLED_BLACK,
            value,
        )

    fun setDynamicColor(value: Boolean) =
        prefs.set(
            Preferences.DYNAMIC_COLOR,
            value,
        )

    fun setCheckForUpdates(value: Boolean) =
        prefs.set(
            Preferences.CHECK_FOR_UPDATES,
            value,
        )

    fun setUpdateChannel(value: UpdateChannel) =
        setEnum(
            Preferences.UPDATE_CHANNEL,
            value,
        )

    fun setLastPromptedVersion(value: String?) =
        prefs.set(
            Preferences.LAST_PROMPTED_VERSION,
            value,
        )

    fun setLegacyPairing(value: Boolean) =
        prefs.set(
            Preferences.LEGACY_PAIRING,
            value,
        )

    fun setAuthToken(value: String?) =
        prefs.set(
            Preferences.AUTH_TOKEN,
            value,
        )

    // -------------------------
    // INT ENUM HELPERS
    // -------------------------

    // Extension function to recast Preference<IntEnum> as Preference<Int>
    private fun <E> Preference<E>.asIntPreference(): Preference<Int>
            where E : Enum<E>, E : IntEnum =
        Preference(key, default.value)

    // Extension function to perform reverse lookup on IntEnum preference
    private inline fun <reified E> getEnum(
        pref: Preference<E>
    ): E where E : Enum<E>, E : IntEnum {
        val stored = prefs.get(pref.asIntPreference())
        return enumValues<E>().firstOrNull { it.value == stored } ?: pref.default
    }

    // Extension function to store IntEnum preference as Int
    private fun <E> setEnum(
        pref: Preference<E>,
        value: E,
    ) where E : Enum<E>, E : IntEnum =
        prefs.set(pref.asIntPreference(), value.value)
}
