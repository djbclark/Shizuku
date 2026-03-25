package moe.shizuku.manager.core.data.preferences

import android.content.Context
import android.content.SharedPreferences

class PreferencesRepository(context: Context) {

    companion object {
        const val PREFS_NAME = "settings"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Global delegate for persisting internal flags, etc. from other features
    fun <T> pref(type: SharedPreferences.() -> Preference<T>) = lazy {
        prefs.type()
    }

    // -------------------------
    // BEHAVIOR
    // -------------------------

    val startMode by pref { enum("start_mode", StartMode.WADB) }
    val startOnBoot by pref { boolean("start_on_boot", false) }
    val watchdog by pref { boolean("watchdog", false) }

    // -------------------------
    // WIRELESS DEBUGGING
    // -------------------------

    val tcpMode by pref { boolean("tcp_mode", true) }
    val tcpPort by pref { int("tcp_port", 5555) }
    val autoDisableUsbDebugging by pref { boolean("auto_disable_usb_debugging", false) }
    val legacyPairing by pref { boolean("legacy_pairing", false) }

    // -------------------------
    // APPEARANCE
    // -------------------------

    val language by pref { string("language", null) }
    val theme by pref { enum("theme", Theme.SYSTEM) }
    val amoledBlack by pref { boolean("amoled_black", false) }
    val dynamicColor by pref { boolean("dynamic_color", true) }

    // -------------------------
    // UPDATES
    // -------------------------

    val checkForUpdates by pref { boolean("check_for_updates", true) }
    val updateChannel by pref { enum("update_channel", UpdateChannel.STABLE) }

    // -------------------------
    // ALL PREFS
    // -------------------------

    val all by lazy { prefs.asFlow { this } }

}
