package moe.shizuku.manager.core.preferences.models

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.data.IntEnum
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

enum class Theme(
    @param:AppCompatDelegate.NightMode override val value: Int,
    @param:StringRes override val labelRes: Int
) : IntEnum, ListSelectionItem {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.settings_system),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.string.settings_theme_light),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, R.string.settings_theme_dark);

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO
}