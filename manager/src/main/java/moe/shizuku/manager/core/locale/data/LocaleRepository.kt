package moe.shizuku.manager.core.locale.data

import android.app.LocaleConfig
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.shizuku.manager.core.extensions.toList
import moe.shizuku.manager.core.locale.models.LocaleEntry
import moe.shizuku.manager.core.locale.models.toLocaleEntry
import moe.shizuku.manager.core.platform.device.AndroidVersion
import java.util.Locale

class LocaleRepository(
    private val context: Context,
    private val localeXmlDataSource: LocaleXmlDataSource
) {
    private var cachedLocales: List<Locale>? = null

    private val _localeFlow = MutableStateFlow(getLocale())
    val localeFlow: StateFlow<LocaleEntry> = _localeFlow.asStateFlow()

    fun getLocale(): LocaleEntry =
        AppCompatDelegate.getApplicationLocales().get(0)?.toLocaleEntry()
        ?: LocaleEntry.SystemDefault

    fun getLocaleEntries(): List<LocaleEntry> {
        val locales = getSupportedLocales() ?: return emptyList()

        val sortedLanguages = locales
            .map { it.toLocaleEntry() }
            .sortedBy { it.nameOwnLocale.lowercase() }

        return listOf(LocaleEntry.SystemDefault) + sortedLanguages
    }

    private fun getSupportedLocales(): List<Locale>? {
        cachedLocales?.let { return it }

        val supportedLocales =
            if (AndroidVersion.isAtLeast13) {
                LocaleConfig(context).supportedLocales?.toList()
            } else {
                localeXmlDataSource.getLocales()
            }

        return supportedLocales.also {
            cachedLocales = it
        }
    }

    fun setLocale(locale: LocaleEntry) {
        val tag = locale.tag
        val locale = if (tag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locale)
        _localeFlow.value = getLocale()
    }
}