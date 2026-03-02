package moe.shizuku.manager.core.ui

import android.annotation.SuppressLint
import android.app.LocaleConfig
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.KeyValueDataSource
import moe.shizuku.manager.core.data.KeyValueEntry
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.capitalize
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

object LocaleHelper {
    private var cachedLocales: List<Locale>? = null

    data class LocaleEntry(
        val nameOwnLocale: String,
        val nameCurrentLocale: String,
        val tag: String
    )

    fun getLocaleEntries(context: Context): List<LocaleEntry> {
        val currentLocale = Locale.getDefault()
        val locales = context.getSupportedLocales()

        val followSystem = LocaleEntry(
            context.getString(R.string.settings_follow_system), "", ""
        )

        val sortedLanguages = locales.map { locale ->
            val nameOwnLocale = locale.getDisplayName(locale)
                .capitalize(locale)
            val nameCurrentLocale = locale.getDisplayName(currentLocale)
                .capitalize(currentLocale)

            LocaleEntry(nameOwnLocale, nameCurrentLocale, locale.toLanguageTag())
        }.sortedBy { it.nameOwnLocale.lowercase() }

        return listOf(followSystem) + sortedLanguages
    }

    fun getLocaleDisplayName(): String? =
        AppCompatDelegate.getApplicationLocales().get(0)?.let { locale ->
            locale.getDisplayName(locale).capitalize(locale)
        }

    fun setLocale(locale: LocaleEntry) =
        setLocale(locale.tag)

    private fun setLocale(tag: String) {
        val locale = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locale)
    }

    private fun Context.getSupportedLocales(): List<Locale> {
        cachedLocales?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleConfig(this).supportedLocales?.let { locales ->
                return locales.toList().also {
                    cachedLocales = it
                }
            }
        }

        return getSupportedLocalesFromXml().also {
            cachedLocales = it
        }
    }

    private fun LocaleList.toList(): List<Locale> {
        return List(size()) { i -> get(i) }
    }

    private fun Context.getSupportedLocalesFromXml(): List<Locale> {
        val locales = mutableListOf<Locale>()
        try {
            // locales_config.xml is generated at build time
            // Thus, the compiler doesn't have access to R.xml.locales_config
            // So, we must use resources.getIdentifier, which is a discouraged API
            @SuppressLint("DiscouragedApi") val resId =
                resources.getIdentifier("locales_config", "xml", packageName).takeUnless { it == 0 }
                    ?: run {
                        Log.e(TAG, "locales_config.xml not found")
                        return emptyList()
                    }

            val xpp = resources.getXml(resId)
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.name == "locale") {
                    val name = xpp.getAttributeValue(
                        "http://schemas.android.com/apk/res/android", "name"
                    )
                    if (name != null) {
                        locales.add(Locale.forLanguageTag(name))
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading locales from XML", e)
        }

        return locales.also { cachedLocales = it }
    }

    private val LANGUAGE_MIGRATED = KeyValueEntry<Boolean>(
        key = "language_migrated",
        default = false,
    )

    // https://developer.android.com/guide/topics/resources/app-languages#custom-storage
    fun migrate() {
        val isMigrated = KeyValueDataSource.get(LANGUAGE_MIGRATED)
        if (isMigrated) return

        // Handles switching all users from custom storage to system storage
        val language = PreferencesRepository.getLanguage()
        if (language != null) {
            val tag = language.takeUnless { it.lowercase() == "system" } ?: ""
            setLocale(tag)
            PreferencesRepository.setLanguage(null)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handles user upgrading from Android 12 to 13+
            if (language == null) {
                val currentLocale = AppCompatDelegate.getApplicationLocales()
                AppCompatDelegate.setApplicationLocales(currentLocale)
            }

            // Migration is finished only for users with Android 13+
            KeyValueDataSource.set(LANGUAGE_MIGRATED, true)
        }
    }
}
