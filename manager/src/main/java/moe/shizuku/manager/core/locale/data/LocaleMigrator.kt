package moe.shizuku.manager.core.locale.data

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import moe.shizuku.manager.core.locale.models.toLocaleEntry
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.preferences.data.boolean

class LocaleMigrator(
    private val preferencesRepository: PreferencesRepository,
    private val localeRepository: LocaleRepository
) {
    private val languageMigrated by preferencesRepository.pref {
        boolean(
            "language_migrated", false
        )
    }

    // https://developer.android.com/guide/topics/resources/app-languages#custom-storage
    fun migrate() {
        if (languageMigrated.get()) return

        // Handles switching all users from custom storage to system storage
        val language = preferencesRepository.language.get()
        if (language != null) {
            val tag = language.takeUnless { it.equals("system", ignoreCase = true) } ?: ""
            localeRepository.setLocale(tag.toLocaleEntry())
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