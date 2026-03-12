package moe.shizuku.manager.core.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.utils.EnvironmentUtils

object ThemeHelper {
    private data class ThemeState(
        val theme: Theme,
        val amoled: Boolean,
        val dynamic: Boolean
    )

    fun applyTheme(activity: AppCompatActivity) {
        val amoledBlack = PreferencesRepository.amoledBlack.value || EnvironmentUtils.isWatch()
        val dynamicColor = PreferencesRepository.dynamicColor.value

        activity.setTheme(R.style.AppTheme)

        if (dynamicColor) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }

        val config = activity.resources.configuration
        val isNightMode =
            (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        if (isNightMode && amoledBlack) {
            activity.theme.applyStyle(R.style.ThemeOverlay_Black, true)
        }
    }

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is AppCompatActivity) {
                    applyTheme(activity)
                    observe(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun observe(activity: AppCompatActivity) {
        activity.lifecycleScope.launch {
            combine(
                PreferencesRepository.theme.flow,
                PreferencesRepository.amoledBlack.flow,
                PreferencesRepository.dynamicColor.flow
            ) { theme, amoled, dynamic ->
                ThemeState(theme, amoled, dynamic)
            }
                .drop(1) // Skip initial value to avoid recreation on launch
                .distinctUntilChanged() // Only trigger if the combination actually changes
                .collect { state ->
                    activity.window.setWindowAnimations(android.R.style.Animation_Dialog)

                    val currentNightMode = AppCompatDelegate.getDefaultNightMode()
                    if (currentNightMode != state.theme.value) {
                        AppCompatDelegate.setDefaultNightMode(state.theme.value)
                    } else {
                        activity.recreate()
                    }
                }
        }
    }

    @ColorInt
    fun resolveColor(context: Context, @AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    fun applySnackbarTheme(context: Context, snackbar: Snackbar) {
        snackbar.setBackgroundTint(resolveColor(context, R.attr.colorPrimaryContainer))
            .setTextColor(resolveColor(context, R.attr.colorOnSurface))
            .setActionTextColor(resolveColor(context, R.attr.colorPrimary))
    }
}