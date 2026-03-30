package moe.shizuku.manager.core.ui.helpers

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.data.preferences.Theme
import moe.shizuku.manager.core.extensions.isWatch

class ThemeHelper(
    private val application: Application,
    private val preferencesRepository: PreferencesRepository
) {

    companion object {
        fun Resources.isNightMode() =
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private data class ThemeState(
        val theme: Theme,
        val amoled: Boolean,
        val dynamic: Boolean
    )

    fun applyTheme(activity: AppCompatActivity) {
        val amoledBlack =
            preferencesRepository.amoledBlack.get() || application.applicationContext.isWatch
        val dynamicColor = preferencesRepository.dynamicColor.get()

        activity.setTheme(R.style.Theme_App)

        if (dynamicColor) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }

        val res = activity.resources
        if (res.isNightMode() && amoledBlack) {
            activity.theme.applyStyle(R.style.ThemeOverlay_App_AmoledBlack, true)
        }
    }

    init {
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
                preferencesRepository.theme.flow,
                preferencesRepository.amoledBlack.flow,
                preferencesRepository.dynamicColor.flow
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
}
