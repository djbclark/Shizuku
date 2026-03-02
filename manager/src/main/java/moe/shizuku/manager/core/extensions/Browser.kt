package moe.shizuku.manager.core.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.components.toast

fun Context.openUrl(
    url: String,
) {
    val uri = url.toUri()

    runCatching {
        launchCustomTab(uri)
    }.recoverCatching {
        launchBrowser(uri)
    }.recoverCatching {
        toast(R.string.error_no_browser_app)
        copyToClipboard(url)
    }
}

private fun Context.launchCustomTab(
    uri: Uri,
) = CustomTabsIntent
    .Builder()
    .setShowTitle(true)
    .build()
    .launchUrl(this, uri)

private fun Context.launchBrowser(
    uri: Uri
) = startActivity(
    Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
)
