package moe.shizuku.manager.core.di

import moe.shizuku.manager.core.adb.AdbPortHelper
import moe.shizuku.manager.core.adb.AdbSession
import moe.shizuku.manager.core.adb.AdbSettingsManager
import moe.shizuku.manager.core.android.DeviceHelper
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionViewModel
import moe.shizuku.manager.core.ui.helpers.LocaleHelper
import moe.shizuku.manager.core.ui.helpers.ThemeHelper
import moe.shizuku.manager.core.utils.ApkSigner
import moe.shizuku.manager.core.utils.ApkUtils
import moe.shizuku.manager.core.utils.AppIconCache
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.core.utils.ShizukuSystemApis
import moe.shizuku.manager.core.utils.UserHandleCompat
import moe.shizuku.manager.home.HomeViewModel
import moe.shizuku.manager.intents.data.TokenRepository
import moe.shizuku.manager.intents.ui.IntentsViewModel
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.ui.authorizedapps.AuthorizedAppsViewModel
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.ShizukuReceiverStarter
import moe.shizuku.manager.privilegedservice.StartOnBootManager
import moe.shizuku.manager.privilegedservice.ui.pairing.AdbPairingViewModel
import moe.shizuku.manager.privilegedservice.ui.start.StartViewModel
import moe.shizuku.manager.settings.ui.SettingsViewModel
import moe.shizuku.manager.shell.ShellBinderRequestHandler
import moe.shizuku.manager.stealth.ui.StealthViewModel
import moe.shizuku.manager.tcpmode.TcpManager
import moe.shizuku.manager.updater.UpdateHelper
import moe.shizuku.manager.updater.data.ReleaseRemoteDataSource
import moe.shizuku.manager.updater.data.ReleaseRepository
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.watchdog.WatchdogManager
import moe.shizuku.manager.watchdog.utils.WatchdogNotifications
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

val appModule = module {
    single<PreferencesRepository>()
    single<PowerManagerHelper>()
    single<WatchdogManager>()
    single<StartOnBootManager>()
    single<ReleaseRemoteDataSource>()
    single<ReleaseRepository>()
    single<LocaleHelper>()
    single<ThemeHelper>()
    single<EnvironmentUtils>()
    single<TokenRepository>()
    single<UpdateHelper>()
    single<ShizukuStateMachine>()
    single<ShizukuSystemApis>()
    single<PermissionManager>()
    single<ShizukuReceiverStarter>()
    single<PrivilegedServiceManager>()
    single<AppIconCache>()
    single<ApkSigner>()
    single<ApkUtils>()
    single<DeviceHelper>()
    single<AdbSettingsManager>()
    single<AdbPortHelper>()
    single<ShellBinderRequestHandler>()
    single<WatchdogNotifications>()
    single<UserHandleCompat>()
    single<TcpManager>()

    factory<AdbSession.Factory>()

    viewModel<SettingsViewModel>()
    viewModel<ListSelectionViewModel>()
    viewModel<HomeViewModel>()
    viewModel<StartViewModel>()
    viewModel<StealthViewModel>()
    viewModel<AuthorizedAppsViewModel>()
    viewModel<IntentsViewModel>()
    viewModel<AdbPairingViewModel>()
}
