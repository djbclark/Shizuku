package moe.shizuku.manager.core.extensions

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(
        this,
        message,
        if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()
}

fun Context.toast(id: Int, long: Boolean = false) =
    toast(getString(id), long)

fun Fragment.toast(id: Int, long: Boolean = false) =
    requireContext().toast(id, long)

fun Fragment.toast(message: String, long: Boolean = false) =
    requireContext().toast(message, long)