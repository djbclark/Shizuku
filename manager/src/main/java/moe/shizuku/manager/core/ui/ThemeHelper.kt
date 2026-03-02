package moe.shizuku.manager.core.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.google.android.material.snackbar.Snackbar
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.utils.EnvironmentUtils

object ThemeHelper {

    private const val THEME_DEFAULT = "DEFAULT"
    private const val THEME_BLACK = "BLACK"

    fun isBlackNightTheme() =
        EnvironmentUtils.isWatch() || PreferencesRepository.getAmoledBlack()

    fun isUsingSystemColor() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && PreferencesRepository.getDynamicColor()

    fun getTheme(context: Context): String {
        val config = context.resources.configuration
        val isNightMode = (config.uiMode and Configuration.UI_MODE_NIGHT_YES) > 0
        if (isBlackNightTheme() && isNightMode) {
            return THEME_BLACK
        }
        return THEME_DEFAULT
    }

    @StyleRes
    fun getThemeStyleRes(context: Context): Int {
        return if (getTheme(context) == THEME_BLACK) {
            R.style.ThemeOverlay_Black
        } else {
            R.style.ThemeOverlay
        }
    }

    fun applySnackbarTheme(context: Context, snackbar: Snackbar) {
        snackbar.setBackgroundTint(resolveColor(context, R.attr.colorPrimaryContainer))
            .setTextColor(resolveColor(context, R.attr.colorOnSurface))
            .setActionTextColor(resolveColor(context, R.attr.colorPrimary))
    }

    @ColorInt
    fun resolveColor(context: Context, @AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }
}
