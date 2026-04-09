package moe.shizuku.manager.core.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import moe.shizuku.manager.R

fun Fragment.openUrl(url: String): Unit = requireContext().openUrl(url)

fun Context.openUrl(url: String) {
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

private fun Context.launchCustomTab(uri: Uri) = CustomTabsIntent
    .Builder()
    .setShowTitle(true)
    .build()
    .launchUrl(this, uri)

private fun Context.launchBrowser(uri: Uri) = startActivity(
    Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
)
