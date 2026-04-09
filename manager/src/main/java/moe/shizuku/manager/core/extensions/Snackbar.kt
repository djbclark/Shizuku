package moe.shizuku.manager.core.extensions

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.snackbar(msg: CharSequence, duration: Int = Snackbar.LENGTH_SHORT): Snackbar =
    Snackbar.make(requireView(), msg, duration)

fun Fragment.snackbar(@StringRes msg: Int, duration: Int = Snackbar.LENGTH_SHORT): Snackbar =
    snackbar(getString(msg), duration)

fun Fragment.showSnackbar(@StringRes msg: Int, duration: Int = Snackbar.LENGTH_SHORT): Unit =
    snackbar(msg, duration).show()