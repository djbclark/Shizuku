package moe.shizuku.manager.core.data.preferences

import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.IntEnum

enum class StartMode(
    override val value: Int,
    @get:StringRes val labelRes: Int
) : IntEnum {
    PC(0, R.string.pc),
    WADB(1, R.string.wireless_debugging),
    ROOT(2, R.string.root)
}

enum class Theme(
    override val value: Int,
    @get:StringRes val labelRes: Int
) : IntEnum {
    SYSTEM(-1, R.string.follow_system),
    LIGHT(1, R.string.settings_theme_light),
    DARK(2, R.string.settings_theme_dark)
}

enum class UpdateChannel(
    override val value: Int,
    @get:StringRes val labelRes: Int
) : IntEnum {
    STABLE(0, R.string.settings_update_channel_stable),
    BETA(1, R.string.settings_update_channel_beta)
}