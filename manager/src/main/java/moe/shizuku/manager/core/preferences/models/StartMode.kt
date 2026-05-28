package moe.shizuku.manager.core.preferences.models

import moe.shizuku.manager.core.preferences.data.IntEnum

enum class StartMode(
    override val value: Int
) : IntEnum {
    WADB(0),
    ROOT(1)
}