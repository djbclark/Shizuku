package moe.shizuku.manager.core.preferences.data

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

// Preference enums should implement this interface
// The value property represents the int that will be persisted in SharedPreferences
interface IntEnum {
    val value: Int
}

// A class to abstract SharedPreferences access
// Supports boolean, int, and string types, as well as int-backed enums
// Exposes both property accessors and a flow
// Preferences are created using the factory extension methods
class Preference<T>(
    val key: String,
    val default: T,
    private val prefs: SharedPreferences,
    private val getter: SharedPreferences.(String, T) -> T,
    private val setter: SharedPreferences.(String, T) -> Unit
) {
    val flow: Flow<T> = prefs.asFlow(key, ::get)
        .distinctUntilChanged()

    fun get(): T = prefs.getter(key, default)

    fun set(v: T): Unit = prefs.setter(key, v)
}

// Factory methods for Preference class
fun SharedPreferences.boolean(key: String, default: Boolean = false): Preference<Boolean> =
    Preference(key, default, this, SharedPreferences::getBoolean) { k, v ->
        edit { putBoolean(k, v) }
    }

fun SharedPreferences.int(key: String, default: Int = 0): Preference<Int> =
    Preference(key, default, this, SharedPreferences::getInt) { k, v ->
        edit { putInt(k, v) }
    }

fun SharedPreferences.string(key: String, default: String? = null): Preference<String?> =
    Preference(key, default, this, SharedPreferences::getString) { k, v ->
        edit { putString(k, v) }
    }

inline fun <reified T> SharedPreferences.enum(
    key: String,
    default: T
): Preference<T> where T : Enum<T>, T : IntEnum =
    Preference(
        key = key,
        default = default,
        prefs = this,
        getter = { k, d ->
            val stored = getInt(k, d.value)
            enumValues<T>().firstOrNull { it.value == stored } ?: d
        },
        setter = { k, v ->
            edit { putInt(k, v.value) }
        }
    )

fun <T> SharedPreferences.asFlow(key: String? = null, valueProvider: () -> T): Flow<T> =
    callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (k == key || key == null) {
                trySend(valueProvider())
            }
        }

        trySend(valueProvider())
        registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            unregisterOnSharedPreferenceChangeListener(listener)
        }
    }