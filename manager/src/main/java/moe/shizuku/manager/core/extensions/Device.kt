package moe.shizuku.manager.core.extensions

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

val Context.isWatch: Boolean
    get() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_WATCH
    }

val Context.isTelevision: Boolean
    get() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
                || packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }