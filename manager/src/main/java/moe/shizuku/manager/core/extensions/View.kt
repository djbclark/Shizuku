package moe.shizuku.manager.core.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams

fun View.applySystemBarsPadding(
    top: Boolean = false,
    bottom: Boolean = false,
    start: Boolean = false,
    end: Boolean = false
) {
    if (this is ViewGroup) {
        clipToPadding = false
    }

    val initialStart = paddingStart
    val initialTop = paddingTop
    val initialEnd = paddingEnd
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(Type.systemBars() or Type.displayCutout())
        v.setPadding(
            if (start) systemBars.left + initialStart else v.paddingLeft,
            if (top) systemBars.top + initialTop else v.paddingTop,
            if (end) systemBars.right + initialEnd else v.paddingRight,
            if (bottom) systemBars.bottom + initialBottom else v.paddingBottom
        )
        insets
    }
}

fun View.applySystemBarsMargin(
    top: Boolean = false,
    bottom: Boolean = false,
    start: Boolean = false,
    end: Boolean = false
) {
    val initialStart = marginStart
    val initialTop = marginTop
    val initialEnd = marginEnd
    val initialBottom = marginBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(Type.systemBars() or Type.displayCutout())
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            if (start) marginStart = systemBars.left + initialStart
            if (top) topMargin = systemBars.top + initialTop
            if (end) marginEnd = systemBars.right + initialEnd
            if (bottom) bottomMargin = systemBars.bottom + initialBottom
        }
        insets
    }
}