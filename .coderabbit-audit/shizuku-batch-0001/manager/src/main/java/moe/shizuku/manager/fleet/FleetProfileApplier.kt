package moe.shizuku.manager.fleet

import android.content.Context
import android.net.Uri
import moe.shizuku.manager.ShizukuSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FleetProfileApplier {

    private val knownKeys = setOf(
        "mode", "start_on_boot", "watchdog", "tcp_mode", "tcp_port",
        "auto_disable_usb_debugging", "legacy_pairing", "update_mode",
    )

    data class Result(
        val success: Boolean,
        val appliedCount: Int,
        val skippedCount: Int,
        val errors: List<String>,
        val message: String,
    )

    @JvmStatic
    fun applyJson(context: Context, json: String): Result {
        return try {
            val profile = JSONObject(json)
            applyProfile(context, profile)
        } catch (e: Exception) {
            Result(false, 0, 0, listOf(e.message ?: "Invalid JSON"), "Profile parse failed: ${e.message}")
        }
    }

    @JvmStatic
    fun applyFromPath(context: Context, path: String): Result {
        return try {
            val json = File(path).readText(Charsets.UTF_8)
            applyJson(context, json)
        } catch (e: Exception) {
            Result(false, 0, 0, listOf(e.message ?: "Read error"), "Failed to read $path: ${e.message}")
        }
    }

    @JvmStatic
    fun applyFromUri(context: Context, uri: Uri): Result {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return Result(false, 0, 0, listOf("Cannot open URI"), "Cannot open URI: $uri")
            val json = stream.use { it.reader(Charsets.UTF_8).readText() }
            applyJson(context, json)
        } catch (e: Exception) {
            Result(false, 0, 0, listOf(e.message ?: "URI error"), "Failed to read URI $uri: ${e.message}")
        }
    }

    private fun applyProfile(context: Context, profile: JSONObject): Result {
        val errors = mutableListOf<String>()
        val prefs = ShizukuSettings.getPreferences()
        val clearExisting = profile.optJSONObject("_meta")?.optBoolean("clear_existing", false) ?: false

        var applied = 0
        var skipped = 0

        val editor = prefs.edit()
        if (clearExisting) {
            editor.clear()
        }

        val keys = profile.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key.startsWith("_")) continue
            if (key !in knownKeys) {
                skipped++
                errors.add("Unknown key: $key")
                continue
            }

            try {
                when (key) {
                    "mode" -> editor.putInt(key, parseLaunchMode(profile.get(key)))
                    "update_mode" -> editor.putInt(key, parseUpdateMode(profile.get(key)))
                    "tcp_port" -> editor.putString(key, parseTcpPort(profile.get(key)))
                    "start_on_boot" -> ShizukuSettings.setStartOnBoot(context, profile.getBoolean(key))
                    "watchdog" -> ShizukuSettings.setWatchdog(context, profile.getBoolean(key))
                    else -> putValue(editor, key, profile.get(key))
                }
                applied++
            } catch (e: Exception) {
                skipped++
                errors.add("$key: ${e.message}")
            }
        }

        editor.apply()

        val message = "Applied $applied preferences, skipped $skipped" +
                if (errors.isEmpty()) "" else " (${errors.size} errors)"

        return Result(errors.isEmpty(), applied, skipped, errors, message)
    }

    private fun parseLaunchMode(value: Any): Int = when (value) {
        is Int -> value
        is String -> when (value.lowercase()) {
            "unknown", "none" -> ShizukuSettings.LaunchMethod.UNKNOWN
            "root" -> ShizukuSettings.LaunchMethod.ROOT
            "adb" -> ShizukuSettings.LaunchMethod.ADB
            else -> throw IllegalArgumentException("Unknown launch mode: $value")
        }
        else -> throw IllegalArgumentException("Unsupported type for mode: ${value.javaClass.simpleName}")
    }

    private fun parseUpdateMode(value: Any): Int = when (value) {
        is Int -> value
        is String -> when (value.lowercase()) {
            "off" -> ShizukuSettings.UpdateMode.OFF
            "stable" -> ShizukuSettings.UpdateMode.STABLE
            "beta" -> ShizukuSettings.UpdateMode.BETA
            else -> throw IllegalArgumentException("Unknown update mode: $value")
        }
        else -> throw IllegalArgumentException("Unsupported type for update_mode: ${value.javaClass.simpleName}")
    }

    private fun parseTcpPort(value: Any): String = when (value) {
        is Int -> value.toString()
        is String -> value
        else -> throw IllegalArgumentException("Unsupported type for tcp_port: ${value.javaClass.simpleName}")
    }

    private fun putValue(editor: android.content.SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Double -> editor.putFloat(key, value.toFloat())
            is Float -> editor.putFloat(key, value)
            is JSONArray -> {
                val set = LinkedHashSet<String>()
                for (i in 0 until value.length()) {
                    set.add(value.getString(i))
                }
                editor.putStringSet(key, set)
            }
            else -> throw IllegalArgumentException("Unsupported type: ${value?.javaClass?.simpleName}")
        }
    }
}
