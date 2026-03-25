package moe.shizuku.manager.core.ui.helpers

import android.annotation.SuppressLint
import android.app.LocaleConfig
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.boolean
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.capitalize
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class LocaleHelper(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private var cachedLocales: List<Locale>? = null

    data class LocaleEntry(
        val nameOwnLocale: String,
        val nameCurrentLocale: String,
        val tag: String
    ) : ListSelectionItem {
        override val label: CharSequence?
            get() = nameOwnLocale.takeUnless { it.isBlank() }

        override val labelRes: Int
            get() = if (nameOwnLocale.isBlank()) R.string.settings_system else 0

        override val description: CharSequence?
            get() = nameCurrentLocale.takeUnless { it.isBlank() }

        override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO
    }

    val systemDefaultEntry = LocaleEntry("", "", "")

    fun getLocaleEntries(): List<LocaleEntry> {
        val locales = context.getSupportedLocales()

        val sortedLanguages = locales.map { it.toLocaleEntry() }
            .sortedBy { it.nameOwnLocale.lowercase() }

        return listOf(systemDefaultEntry) + sortedLanguages
    }

    fun getLocale(): LocaleEntry =
        AppCompatDelegate.getApplicationLocales().get(0)?.toLocaleEntry()
            ?: systemDefaultEntry

    private val _localeFlow = MutableStateFlow(getLocale())
    val localeFlow: StateFlow<LocaleEntry> = _localeFlow.asStateFlow()

    fun setLocale(locale: LocaleEntry) {
        setLocale(locale.tag)
        _localeFlow.value = getLocale()
    }

    private fun setLocale(tag: String) {
        val locale = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locale)
    }

    private fun Locale.toLocaleEntry(): LocaleEntry {
        val currentLocale = Locale.getDefault()
        return LocaleEntry(
            nameOwnLocale = getDisplayName(this).capitalize(this),
            nameCurrentLocale = getDisplayName(currentLocale).capitalize(currentLocale),
            tag = toLanguageTag()
        )
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
                resources.getIdentifier("_generated_res_locale_config", "xml", packageName)
                    .takeUnless { it == 0 }
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

        return locales
    }

    private val languageMigrated by preferencesRepository.pref { boolean("language_migrated", false) }

    // https://developer.android.com/guide/topics/resources/app-languages#custom-storage
    fun migrate() {
        if (languageMigrated.get()) return

        // Handles switching all users from custom storage to system storage
        val language = preferencesRepository.language.get()
        if (language != null) {
            val tag = language.takeUnless { it.lowercase() == "system" } ?: ""
            setLocale(tag)
            preferencesRepository.language.set(null)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handles user upgrading from Android 12 to 13+
            if (language == null) {
                val currentLocale = AppCompatDelegate.getApplicationLocales()
                AppCompatDelegate.setApplicationLocales(currentLocale)
            }

            // Migration is finished only for users with Android 13+
            languageMigrated.set(true)
        }
    }
}
