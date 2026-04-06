package moe.shizuku.manager

import android.app.Application
import android.os.Build
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.core.di.appModule
import moe.shizuku.manager.core.ui.helpers.LocaleHelper
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

class ShizukuApplication : Application() {
    private val localeHelper: LocaleHelper by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@ShizukuApplication)
            modules(appModule)
        }

        init()
    }

    private fun init() {
        Shell.setDefaultBuilder(Shell.Builder.create())
        if (Build.VERSION.SDK_INT >= 28) {
            HiddenApiBypass.setHiddenApiExemptions("")
        }
        if (Build.VERSION.SDK_INT >= 30) {
            System.loadLibrary("adb")
        }

        localeHelper.migrate()
    }
}
