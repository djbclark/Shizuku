package moe.shizuku.manager.core.data.preferences

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import moe.shizuku.manager.R

enum class StartMode(
    override val value: Int,
    @get:StringRes val labelRes: Int
) : IntEnum {
    WADB(0, R.string.wireless_debugging),
    ROOT(1, R.string.root)
}

enum class Theme(
    @get:AppCompatDelegate.NightMode override val value: Int,
    @get:StringRes val labelRes: Int
) : IntEnum {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.settings_system),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.string.settings_theme_light),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, R.string.settings_theme_dark)
}

enum class UpdateChannel(
    override val value: Int,
    @get:StringRes val labelRes: Int
) : IntEnum {
    STABLE(0, R.string.settings_update_channel_stable),
    BETA(1, R.string.settings_update_channel_beta)
}