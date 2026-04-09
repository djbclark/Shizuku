package moe.shizuku.manager.core.preferences.models

import android.os.Build
import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.data.IntEnum
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem
import moe.shizuku.manager.core.utils.EnvironmentUtils

enum class StartMode(
    override val value: Int, @param:StringRes override val labelRes: Int
) : IntEnum, ListSelectionItem {
    WADB(0, R.string.wireless_debugging),
    ROOT(1, R.string.root);

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO

    override val descriptionRes: Int?
        get() = when (this) {
            WADB -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                R.string.wireless_debugging_requirement
            } else {
                R.string.wireless_debugging_requirement_pre_11
            }

            else -> null
        }

    override val isEnabled: Boolean
        get() = when (this) {
            ROOT -> EnvironmentUtils.isRooted()
            else -> true
        }
}