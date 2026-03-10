package moe.shizuku.manager.core.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.databinding.AppbarFragmentActivityBinding

open class MainActivity : AppCompatActivity() {
    private lateinit var binding: AppbarFragmentActivityBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = AppbarFragmentActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        maybeDeepLinkToSettings(intent)
    }

    private fun setupUI() {
        // Appbar Setup
        binding.appbar.toolbarContainer.applySystemBarsPadding(top = true, start = true, end = true)
        setSupportActionBar(binding.appbar.toolbar)

        // Navigation Setup
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        // Connect them
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.home_fragment))
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp() =
        navController.navigateUp() || super.onSupportNavigateUp()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        maybeDeepLinkToSettings(intent)
    }

    private fun maybeDeepLinkToSettings(intent: Intent?) {
        if (intent?.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            navController.navigate(R.id.settings_fragment)
        }
    }
}