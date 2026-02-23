package moe.shizuku.manager.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object PreferencesDataSource {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    // -------------------------
    // GETTERS
    // -------------------------

    fun get(pref: Preference<Boolean>): Boolean =
        prefs.getBoolean(pref.key, pref.default)

    fun get(pref: Preference<Int>): Int =
        prefs.getInt(pref.key, pref.default)

    fun get(pref: Preference<String?>): String? =
        prefs.getString(pref.key, pref.default)

    // -------------------------
    // SETTERS
    // -------------------------

    fun set(pref: Preference<Boolean>, value: Boolean) =
        prefs.edit { putBoolean(pref.key, value) }

    fun set(pref: Preference<Int>, value: Int) =
        prefs.edit { putInt(pref.key, value) }

    fun set(pref: Preference<String?>, value: String?) =
        prefs.edit { putString(pref.key, value) }

}
