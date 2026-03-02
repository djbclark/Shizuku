package moe.shizuku.manager

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.data.KeyValueDataSource
import moe.shizuku.manager.core.data.preferences.PreferenceSync
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.ui.LocaleHelper
import moe.shizuku.manager.watchdog.services.WatchdogManager
import org.lsposed.hiddenapibypass.HiddenApiBypass

class ShizukuApplication : Application() {

    companion object {
        lateinit var application: ShizukuApplication
            private set

        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        appContext = applicationContext
        init(applicationContext)
    }

    private fun init(context: Context) {
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
        if (Build.VERSION.SDK_INT >= 28) {
            HiddenApiBypass.setHiddenApiExemptions("")
        }
        if (Build.VERSION.SDK_INT >= 30) {
            System.loadLibrary("adb")
        }

        injectDependencies(context)
    }

    private fun injectDependencies(context: Context) {
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        // Should be declared first because other dependencies may require key-value storage
        KeyValueDataSource.init(context)

        PowerManagerHelper.init(context)
        WatchdogManager.init(context, applicationScope)
        PreferenceSync.init(context, applicationScope)

        LocaleHelper.migrate()

        AppCompatDelegate.setDefaultNightMode(PreferencesRepository.getTheme().value)
    }

}
