package moe.shizuku.manager.core.locale.data

import android.app.LocaleConfig
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.shizuku.manager.core.extensions.toList
import moe.shizuku.manager.core.locale.models.LocaleEntry
import moe.shizuku.manager.core.locale.models.toLocaleEntry
import java.util.Locale

class LocaleRepository(
    private val context: Context,
    private val localeXmlDataSource: LocaleXmlDataSource
) {
    private var cachedLocales: List<Locale>? = null

    private val _localeFlow = MutableStateFlow(getLocale())
    val localeFlow: StateFlow<LocaleEntry> = _localeFlow.asStateFlow()

    fun getLocale(): LocaleEntry = AppCompatDelegate.getApplicationLocales().get(0)?.toLocaleEntry()
        ?: LocaleEntry.SystemDefault

    fun getLocaleEntries(): List<LocaleEntry> {
        val locales = getSupportedLocales()

        val sortedLanguages =
            locales.map { it.toLocaleEntry() }.sortedBy { it.nameOwnLocale.lowercase() }

        return listOf(LocaleEntry.SystemDefault) + sortedLanguages
    }

    private fun getSupportedLocales(): List<Locale> {
        cachedLocales?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleConfig(context).supportedLocales?.let { locales ->
                return locales.toList().also {
                    cachedLocales = it
                }
            }
        }

        return localeXmlDataSource.getLocales(context).also {
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