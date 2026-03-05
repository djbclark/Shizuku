package moe.shizuku.manager.core.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.databinding.AppbarFragmentActivityBinding
import moe.shizuku.manager.home.HomeFragment

open class MainActivity : AppCompatActivity() {

    private lateinit var binding: AppbarFragmentActivityBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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