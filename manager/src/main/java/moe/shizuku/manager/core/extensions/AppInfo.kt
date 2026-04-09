package moe.shizuku.manager.core.extensions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import coil3.load
import coil3.request.crossfade
import coil3.request.placeholder
import moe.shizuku.manager.R

fun ImageView.setAppIcon(appInfo: ApplicationInfo) {
    val icon = context.packageManager.getApplicationIcon(appInfo)
    val placeholderDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_app_placeholder)

    load(icon) {
        crossfade(true)
        placeholder(placeholderDrawable)
    }
}

fun Context.getAppLabel(appInfo: ApplicationInfo): String =
    packageManager.getApplicationLabel(appInfo).toString()
