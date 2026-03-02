package moe.shizuku.manager.core.ui

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.databinding.AppbarFragmentActivityBinding
import moe.shizuku.manager.home.HomeFragment
import rikka.material.app.MaterialActivity

open class MainActivity : MaterialActivity() {

    private lateinit var binding: AppbarFragmentActivityBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun computeUserThemeKey(): String {
        return ThemeHelper.getTheme(this) + ThemeHelper.isUsingSystemColor()
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        if (ThemeHelper.isUsingSystemColor()) {
            val config = resources.configuration
            if (config.uiMode and Configuration.UI_MODE_NIGHT_YES == Configuration.UI_MODE_NIGHT_YES) {
                theme.applyStyle(R.style.ThemeOverlay_DynamicColors_Dark, true)
            } else {
                theme.applyStyle(R.style.ThemeOverlay_DynamicColors_Light, true)
            }
        }
        theme.applyStyle(ThemeHelper.getThemeStyleRes(this), true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AppbarFragmentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appbar.toolbarContainer.applySystemBarsPadding(top = true, start = true, end = true)

        setSupportActionBar(binding.appbar.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController

        if (intent?.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            navController.navigate(R.id.settings_fragment)
        }

        appBarConfiguration = AppBarConfiguration(setOf(R.id.home_fragment))
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        val fragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
        if (fragment is HomeFragment) {
            fragment.onNewIntent(intent)
        }
    }
}