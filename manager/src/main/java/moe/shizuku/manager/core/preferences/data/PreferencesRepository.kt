package moe.shizuku.manager.core.preferences.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import moe.shizuku.manager.core.preferences.models.StartMode
import moe.shizuku.manager.core.preferences.models.Theme
import moe.shizuku.manager.core.preferences.models.UpdateChannel
import moe.shizuku.manager.core.utils.EnvironmentUtils

class PreferencesRepository(context: Context) {

    companion object {
        const val PREFS_NAME: String = "settings"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Global delegate for persisting internal flags, etc. from other features
    fun <T> pref(type: SharedPreferences.() -> Preference<T>): Lazy<Preference<T>> = lazy {
        prefs.type()
    }

    // -------------------------
    // BEHAVIOR
    // -------------------------

    val startMode: Preference<StartMode> by pref {
        enum(
            "start_mode",
            if (EnvironmentUtils.isRooted()) StartMode.ROOT else StartMode.WADB
        )
    }
    val startOnBoot: Preference<Boolean> by pref { boolean("start_on_boot") }
    val watchdog: Preference<Boolean> by pref { boolean("watchdog") }

    // -------------------------
    // WIRELESS DEBUGGING
    // -------------------------

    val tcpMode: Preference<Boolean> by pref { boolean("tcp_mode", true) }
    val tcpPort: Preference<Int> by pref { int("tcp_port", 5555) }
    val autoDisableUsbDebugging: Preference<Boolean> by pref { boolean("auto_disable_usb_debugging") }
    val legacyPairing: Preference<Boolean> by pref { boolean("legacy_pairing") }

    // -------------------------
    // APPEARANCE
    // -------------------------

    val language: Preference<String?> by pref { string("language") }
    val theme: Preference<Theme> by pref { enum("theme", Theme.SYSTEM) }
    val amoledBlack: Preference<Boolean> by pref { boolean("amoled_black") }
    val dynamicColor: Preference<Boolean> by pref { boolean("dynamic_color", true) }

    // -------------------------
    // UPDATES
    // -------------------------

    val checkForUpdates: Preference<Boolean> by pref { boolean("check_for_updates", true) }
    val updateChannel: Preference<UpdateChannel> by pref { enum("update_channel", UpdateChannel.STABLE) }

    // -------------------------
    // ALL PREFS
    // -------------------------

    val all: Flow<PreferencesRepository> by lazy { prefs.asFlow { this } }

}
