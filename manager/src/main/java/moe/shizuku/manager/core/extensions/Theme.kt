package moe.shizuku.manager.core.extensions

import android.content.res.Configuration
import android.content.res.Resources

val Resources.isNightMode: Boolean
    get() = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES