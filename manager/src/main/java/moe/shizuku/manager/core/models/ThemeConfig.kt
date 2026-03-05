package moe.shizuku.manager.core.models

import moe.shizuku.manager.core.data.preferences.Theme

data class ThemeConfig(
    val theme: Theme,
    val amoledBlack: Boolean,
    val dynamicColor: Boolean
)