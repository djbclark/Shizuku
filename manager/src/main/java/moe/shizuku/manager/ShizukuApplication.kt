package moe.shizuku.manager

import android.app.Application
import android.content.Context
import android.os.Build
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.core.ui.helpers.LocaleHelper
import moe.shizuku.manager.core.di.appModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

class ShizukuApplication : Application() {

    companion object {
        lateinit var application: ShizukuApplication
            private set

        lateinit var appContext: Context
            private set
    }

    private val localeHelper: LocaleHelper by inject()

    override fun onCreate() {
        super.onCreate()
        application = this
        appContext = applicationContext
        
        startKoin {
            androidLogger()
            androidContext(this@ShizukuApplication)
            modules(appModule)
        }

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

        localeHelper.migrate()
    }
}
