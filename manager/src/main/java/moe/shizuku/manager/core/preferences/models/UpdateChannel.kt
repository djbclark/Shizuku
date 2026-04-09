package moe.shizuku.manager.core.preferences.models

import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.data.IntEnum
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

enum class UpdateChannel(
    override val value: Int, @param:StringRes override val labelRes: Int
) : IntEnum, ListSelectionItem {
    STABLE(0, R.string.settings_update_channel_stable),
    BETA(1, R.string.settings_update_channel_beta);

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO
}