package moe.shizuku.manager.core.ui.components

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import moe.shizuku.manager.core.ui.ThemeHelper

fun Fragment.snackbar(
    msg: CharSequence,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionText: CharSequence? = null,
    action: (() -> Unit)? = null
) {
    val v = view ?: return

    val snackbar = Snackbar.make(v, msg, duration)
    if (action != null) {
        val label = actionText ?: getString(android.R.string.ok)
        snackbar.setAction(label) { action() }
    }

    ThemeHelper.applySnackbarTheme(v.context, snackbar)
    snackbar.show()
}

fun Fragment.snackbar(
    @StringRes msg: Int,
    duration: Int = Snackbar.LENGTH_SHORT,
    @StringRes actionText: Int? = null,
    action: (() -> Unit)? = null
) = snackbar(
    getString(msg),
    duration,
    actionText?.let { getString(it) },
    action
)