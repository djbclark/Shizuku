package moe.shizuku.manager.core.di

import moe.shizuku.manager.autostart.AutoStartManager
import moe.shizuku.manager.autostart.StartOnBootManager
import moe.shizuku.manager.core.locale.data.LocaleMigrator
import moe.shizuku.manager.core.locale.data.LocaleRepository
import moe.shizuku.manager.core.locale.data.LocaleXmlDataSource
import moe.shizuku.manager.core.platform.KeyguardHelper
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSession
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.platform.device.user.DeviceUserRepository
import moe.shizuku.manager.core.platform.settings.PowerManagerHelper
import moe.shizuku.manager.core.platform.userservice.UserServiceRepository
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionViewModel
import moe.shizuku.manager.core.ui.helpers.ThemeHelper
import moe.shizuku.manager.core.utils.ApkSigner
import moe.shizuku.manager.core.utils.ApkUtils
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.home.HomeViewModel
import moe.shizuku.manager.intents.data.TokenRepository
import moe.shizuku.manager.intents.ui.IntentsViewModel
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.data.AuthorizedAppsRepository
import moe.shizuku.manager.permission.ui.authorizedapps.AuthorizedAppsViewModel
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.ui.pairing.AdbPairingViewModel
import moe.shizuku.manager.privilegedservice.ui.start.StartViewModel
import moe.shizuku.manager.settings.ui.SettingsViewModel
import moe.shizuku.manager.shell.ShellBinderRequestHandler
import moe.shizuku.manager.stealth.ui.StealthViewModel
import moe.shizuku.manager.tcpmode.TcpManager
import moe.shizuku.manager.updater.UpdateHelper
import moe.shizuku.manager.updater.data.ReleaseRemoteDataSource
import moe.shizuku.manager.updater.data.ReleaseRepository
import moe.shizuku.manager.privilegedservice.data.ShizukuStateMachine
import moe.shizuku.manager.watchdog.WatchdogManager
import moe.shizuku.manager.watchdog.utils.WatchdogNotifications
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val appModule = module {
    single<AdbPortHelper>()
    single<AdbSettingsManager>()
    single<ApkSigner>()
    single<ApkUtils>()
    single<AuthorizedAppsRepository>()
    single<AutoStartManager>()
    single<DeviceUserRepository>()
    single<EnvironmentUtils>()
    single<KeyguardHelper>()
    single<LocaleMigrator>()
    single<LocaleRepository>()
    single<LocaleXmlDataSource>()
    single<PermissionManager>()
    single<PowerManagerHelper>()
    single<PreferencesRepository>()
    single<PrivilegedServiceManager>()
    single<ReleaseRemoteDataSource>()
    single<ReleaseRepository>()
    single<ShellBinderRequestHandler>()
    single<ShizukuStateMachine>()
    single<StartOnBootManager>()
    single<TcpManager>()
    single<ThemeHelper>() withOptions {
        createdAtStart()
    }
    single<TokenRepository>()
    single<UpdateHelper>()
    single<UserServiceRepository>()
    single<WatchdogManager>()
    single<WatchdogNotifications>()

    factory<AdbSession.Factory>()

    viewModel<AdbPairingViewModel>()
    viewModel<AuthorizedAppsViewModel>()
    viewModel<HomeViewModel>()
    viewModel<IntentsViewModel>()
    viewModel<ListSelectionViewModel>()
    viewModel<SettingsViewModel>()
    viewModel<StartViewModel>()
    viewModel<StealthViewModel>()
}
