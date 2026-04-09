package moe.shizuku.manager

import android.app.Application
import android.os.Build
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.core.di.appModule
import moe.shizuku.manager.core.locale.data.LocaleMigrator
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MainApplication : Application() {
    private val localeMigrator: LocaleMigrator by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
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

        localeMigrator.migrate()
    }
}
