package moe.shizuku.manager.core.extensions

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

fun Context.toast(
    message: String,
    long: Boolean = false
): Unit = Toast.makeText(
    this,
    message,
    if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
).show()

fun Context.toast(
    @StringRes id: Int,
    long: Boolean = false
): Unit = toast(getString(id), long)

fun Fragment.toast(
    message: String,
    long: Boolean = false
): Unit = requireContext().toast(message, long)

fun Fragment.toast(
    @StringRes id: Int,
    long: Boolean = false
): Unit = requireContext().toast(id, long)