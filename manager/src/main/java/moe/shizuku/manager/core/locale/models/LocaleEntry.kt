package moe.shizuku.manager.core.locale.models

import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.capitalize
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem
import java.util.Locale

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

    companion object {
        val SystemDefault = LocaleEntry("", "", "")
    }
}

fun Locale.toLocaleEntry(): LocaleEntry {
    val currentLocale = Locale.getDefault()
    return LocaleEntry(
        nameOwnLocale = getDisplayName(this).capitalize(this),
        nameCurrentLocale = getDisplayName(currentLocale).capitalize(currentLocale),
        tag = toLanguageTag()
    )
}

fun String.toLocaleEntry(): LocaleEntry {
    return if (this.isBlank()) LocaleEntry.SystemDefault
    else Locale.forLanguageTag(this).toLocaleEntry()
}