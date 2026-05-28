package moe.shizuku.manager.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import moe.shizuku.manager.autostart.notifications.AutoStartNotification
import moe.shizuku.manager.autostart.AutoStartManager
import moe.shizuku.manager.autostart.AutoStartWorker
import moe.shizuku.manager.autostart.StartOnBootManager
import moe.shizuku.manager.autostart.notifications.AutoStartNotificationChannel
import moe.shizuku.manager.pairing.notifications.AdbPairingNotification
import moe.shizuku.manager.watchdog.notifications.WatchdogNotification
import moe.shizuku.manager.core.locale.data.LocaleMigrator
import moe.shizuku.manager.core.locale.data.LocaleRepository
import moe.shizuku.manager.core.locale.data.LocaleXmlDataSource
import moe.shizuku.manager.core.platform.settings.SettingsIntentFactory
import moe.shizuku.manager.core.platform.services.KeyguardHelper
import moe.shizuku.manager.core.platform.adb.AdbPortHelper
import moe.shizuku.manager.core.platform.adb.AdbSession
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.platform.adb.AdbMdns
import moe.shizuku.manager.intents.notifications.IntentsErrorNotification
import moe.shizuku.manager.core.platform.services.notifications.AppNotificationChannel
import moe.shizuku.manager.core.platform.services.notifications.NotificationHelper
import moe.shizuku.manager.core.platform.services.packages.installer.PackageInstallerHelper
import moe.shizuku.manager.core.platform.services.packages.manager.PackageInfoRepository
import moe.shizuku.manager.core.platform.services.user.DeviceUserRepository
import moe.shizuku.manager.core.platform.services.BatteryOptimizationHelper
import moe.shizuku.manager.core.platform.services.notifications.NotificationChannelManager
import moe.shizuku.manager.core.platform.services.packages.manager.PackageManagerHelper
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionViewModel
import moe.shizuku.manager.core.ui.helpers.ThemeHelper
import moe.shizuku.manager.core.utils.ApkSigner
import moe.shizuku.manager.core.utils.ApkUtils
import moe.shizuku.manager.home.HomeViewModel
import moe.shizuku.manager.intents.data.TokenRepository
import moe.shizuku.manager.intents.notifications.IntentsNotificationChannel
import moe.shizuku.manager.intents.ui.IntentsViewModel
import moe.shizuku.manager.intents.usecases.ValidateTokenUseCase
import moe.shizuku.manager.pairing.notifications.AdbPairingNotificationChannel
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.data.AuthorizedAppsRepository
import moe.shizuku.manager.permission.ui.authorizedapps.AuthorizedAppsViewModel
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.pairing.ui.AdbPairingViewModel
import moe.shizuku.manager.privilegedservice.PrivilegedServiceStateMachine
import moe.shizuku.manager.privilegedservice.ServiceMetadataRepository
import moe.shizuku.manager.start.ui.StartViewModel
import moe.shizuku.manager.settings.ui.SettingsViewModel
import moe.shizuku.manager.shell.ShellBinderRequestHandler
import moe.shizuku.manager.stealth.ui.StealthViewModel
import moe.shizuku.manager.tcpmode.TcpManager
import moe.shizuku.manager.updater.UpdateHelper
import moe.shizuku.manager.updater.data.ReleaseRemoteDataSource
import moe.shizuku.manager.updater.data.ReleaseRepository
import moe.shizuku.manager.watchdog.notifications.CrashNotification
import moe.shizuku.manager.watchdog.WatchdogManager
import moe.shizuku.manager.watchdog.notifications.CrashNotificationChannel
import moe.shizuku.manager.watchdog.notifications.WatchdogNotificationChannel
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.create
import org.koin.plugin.module.dsl.factory
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel
import org.koin.plugin.module.dsl.worker

val appModule: Module = module {
    fun coroutineScope() = CoroutineScope(Dispatchers.Default + SupervisorJob())
    single { create(::coroutineScope) }

    single<AdbMdns>()
    single<AdbPortHelper>()
    single<AdbSettingsManager>()
    single<ApkSigner>()
    single<ApkUtils>()
    single<AuthorizedAppsRepository>()
    single<AutoStartManager>()
    single<BatteryOptimizationHelper>()
    single<DeviceUserRepository>()
    single<KeyguardHelper>()
    single<LocaleMigrator>()
    single<LocaleRepository>()
    single<LocaleXmlDataSource>()
    single<PackageInfoRepository>()
    single<PackageInstallerHelper>()
    single<PackageManagerHelper>()
    single<SettingsIntentFactory>()
    single<PermissionManager>()
    single<PreferencesRepository>()
    single<PrivilegedServiceManager>()
    single<PrivilegedServiceStateMachine>()
    single<ReleaseRemoteDataSource>()
    single<ReleaseRepository>()
    single<ServiceMetadataRepository>()
    single<ShellBinderRequestHandler>()
    single<StartOnBootManager>()
    single<TcpManager>()
    single<ThemeHelper>()
    single<TokenRepository>()
    single<UpdateHelper>()
    single<WatchdogManager>()

    // Notifications
    single<NotificationHelper>()
    single<NotificationChannelManager>()
    single<AdbPairingNotification>()
    single<IntentsErrorNotification>()
    single<AutoStartNotification>()
    single<CrashNotification>()
    single<WatchdogNotification>()
    single<AdbPairingNotificationChannel>() bind AppNotificationChannel::class
    single<IntentsNotificationChannel>() bind AppNotificationChannel::class
    single<AutoStartNotificationChannel>() bind AppNotificationChannel::class
    single<CrashNotificationChannel>() bind AppNotificationChannel::class
    single<WatchdogNotificationChannel>() bind AppNotificationChannel::class

    factory<AdbSession.Factory>()
    factory<ValidateTokenUseCase>()

    viewModel<AdbPairingViewModel>()
    viewModel<AuthorizedAppsViewModel>()
    viewModel<HomeViewModel>()
    viewModel<IntentsViewModel>()
    viewModel<ListSelectionViewModel>()
    viewModel<SettingsViewModel>()
    viewModel<StartViewModel>()
    viewModel<StealthViewModel>()

    worker<AutoStartWorker>()
}
