package moe.shizuku.manager.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object KeyValueDataSource {
    const val PREFS_NAME = "settings"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // TODO remove function - used for ADB KeyStore
    fun getPreferences() = prefs

    // -------------------------
    // GETTERS
    // -------------------------

    fun get(pref: KeyValueEntry<Boolean>): Boolean =
        prefs.getBoolean(pref.key, pref.default)

    fun get(pref: KeyValueEntry<Int>): Int =
        prefs.getInt(pref.key, pref.default)

    fun get(pref: KeyValueEntry<String?>): String? =
        prefs.getString(pref.key, pref.default)

    // -------------------------
    // FLOWS
    // -------------------------

    @JvmName("observeBoolean")
    fun observe(entry: KeyValueEntry<Boolean>): Flow<Boolean> =
        observe(entry) { get(entry) }

    @JvmName("observeInt")
    fun observe(entry: KeyValueEntry<Int>): Flow<Int> =
        observe(entry) { get(entry) }

    @JvmName("observeString")
    fun observe(entry: KeyValueEntry<String?>): Flow<String?> =
        observe(entry) { get(entry) }

    // Generic flow for all types of preferences
    fun <T> observe(entry: KeyValueEntry<T>, valueProvider: () -> T): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == entry.key) {
                trySend(valueProvider())
            }
        }

        // Emit the initial value
        trySend(valueProvider())

        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    // -------------------------
    // SETTERS
    // -------------------------

    fun set(pref: KeyValueEntry<Boolean>, value: Boolean) =
        prefs.edit { putBoolean(pref.key, value) }

    fun set(pref: KeyValueEntry<Int>, value: Int) =
        prefs.edit { putInt(pref.key, value) }

    fun set(pref: KeyValueEntry<String?>, value: String?) =
        prefs.edit { putString(pref.key, value) }
}