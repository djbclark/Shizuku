package moe.shizuku.manager.shizukuservice.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.adb.AdbInvalidPairingCodeException
import moe.shizuku.manager.core.adb.AdbKey
import moe.shizuku.manager.core.adb.AdbKeyException
import moe.shizuku.manager.core.adb.AdbMdns
import moe.shizuku.manager.core.adb.AdbPairingClient
import moe.shizuku.manager.core.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.home.HomeFragment
import java.net.ConnectException

@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {
    companion object {
        const val NOTIFICATION_CHANNEL = "adb_pairing"
        const val NOTIFICATION_ID = 1

        private const val REPLY_REQUEST_ID = 1
        private const val STOP_REQUEST_ID = 2
        private const val RETRY_REQUEST_ID = 3
        private const val START_ACTION = "start"
        private const val STOP_ACTION = "stop"
        private const val REPLY_ACTION = "reply"
        private const val REMOTE_INPUT_RESULT_KEY = "paring_code"
        private const val PORT_KEY = "paring_code"

        fun startIntent(context: Context): Intent =
            Intent(context, AdbPairingService::class.java).setAction(START_ACTION)

        private fun stopIntent(context: Context): Intent =
            Intent(context, AdbPairingService::class.java).setAction(STOP_ACTION)

        private fun replyIntent(
            context: Context,
            port: Int,
        ): Intent = Intent(context, AdbPairingService::class.java).setAction(REPLY_ACTION)
            .putExtra(PORT_KEY, port)
    }

    private var adbMdns: AdbMdns? = null

    private val observer =
        Observer<Int> { port ->
            Log.i(TAG, "Pairing service port: $port")
            if (port <= 0) return@Observer

            // Since the service could be killed before user finishing input,
            // we need to put the port into Intent
            val notification = createInputNotification(port)

            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        }

    private var started = false

    override fun onCreate() {
        super.onCreate()

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.pairing_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                setShowBadge(false)
                setAllowBubbles(false)
            },
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val notification =
            when (intent?.action) {
                START_ACTION -> {
                    onStart()
                }

                REPLY_ACTION -> {
                    val code = RemoteInput.getResultsFromIntent(intent)
                        ?.getCharSequence(REMOTE_INPUT_RESULT_KEY) ?: ""
                    val port = intent.getIntExtra(PORT_KEY, -1)
                    if (port != -1) {
                        onInput(code.toString(), port)
                    } else {
                        onStart()
                    }
                }

                STOP_ACTION -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    null
                }

                else -> {
                    return START_NOT_STICKY
                }
            }
        if (notification != null) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST,
                )
            } catch (e: Throwable) {
                Log.e(TAG, "startForeground failed", e)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e is ForegroundServiceStartNotAllowedException
                ) {
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID,
                        notification
                    )
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onTimeout(startId: Int) {
        toast(R.string.pairing_timed_out)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSearch() {
        if (started) return
        started = true
        adbMdns = AdbMdns(this, AdbMdns.TLS_PAIRING, observer).apply { start() }
    }

    private fun stopSearch() {
        if (!started) return
        started = false
        adbMdns?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSearch()
    }

    private fun onStart(): Notification {
        startSearch()
        return searchingNotification
    }

    private fun onInput(
        code: String,
        port: Int,
    ): Notification {
        GlobalScope.launch(Dispatchers.IO) {
            val host = "127.0.0.1"

            val key =
                try {
                    AdbKey(PreferenceAdbKeyStore(PreferencesRepository.prefs), "shizuku")
                } catch (e: Throwable) {
                    e.printStackTrace()
                    return@launch
                }

            AdbPairingClient(host, port, code, key)
                .runCatching {
                    start()
                }.onFailure {
                    handleResult(false, it)
                }.onSuccess {
                    handleResult(it, null)
                }
        }

        return workingNotification
    }

    private fun handleResult(
        success: Boolean,
        exception: Throwable?,
    ) {
        stopForeground(STOP_FOREGROUND_DETACH)

        val title: String
        val text: String?

        if (success) {
            Log.i(TAG, "Pair succeed")

            title = getString(R.string.pairing_successful)
            text = getString(R.string.pairing_successful_message)

            stopSearch()
        } else {
            title = getString(R.string.pairing_failed)

            text =
                when (exception) {
                    is ConnectException -> {
                        getString(R.string.start_error_connection)
                    }

                    is AdbInvalidPairingCodeException -> {
                        getString(R.string.pairing_error_invalid_code)
                    }

                    is AdbKeyException -> {
                        getString(R.string.adb_error_key_store)
                    }

                    else -> {
                        exception?.let { Log.getStackTraceString(it) }
                    }
                }

            if (exception != null) {
                Log.w(TAG, "Pair failed", exception)
            } else {
                Log.w(TAG, "Pair failed")
            }
        }

        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            Notification
                .Builder(this, NOTIFICATION_CHANNEL)
                .setColor(MaterialColors.getColor(this, android.R.attr.colorPrimary, 0))
                .setSmallIcon(R.drawable.ic_system_icon)
                .setContentTitle(title)
                .setContentText(text)
                .apply {
                    if (!success) {
                        addAction(retryNotificationAction)
                    } else {
                        setContentIntent(launchIntent.createPendingIntent())
                        addAction(startNotificationAction)
                        setAutoCancel(true)
                    }
                }.build(),
        )
        stopSelf()
    }

    private val launchIntent by lazy {
        NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.home_fragment)
    }

    private val startNotificationAction by lazy {
        val pendingIntent =
            launchIntent
                .setArguments(bundleOf(HomeFragment.ARG_START_SERVICE to true))
                .createPendingIntent()

        Notification.Action
            .Builder(
                null,
                getString(R.string.start),
                pendingIntent,
            ).build()
    }

    private val stopNotificationAction by lazy {
        val pendingIntent =
            PendingIntent.getService(
                this,
                STOP_REQUEST_ID,
                stopIntent(this),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                },
            )

        Notification.Action
            .Builder(
                null,
                getString(R.string.pairing_stop_searching),
                pendingIntent,
            ).build()
    }

    private val retryNotificationAction by lazy {
        val pendingIntent =
            PendingIntent.getService(
                this,
                RETRY_REQUEST_ID,
                startIntent(this),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                },
            )

        Notification.Action
            .Builder(
                null,
                getString(R.string.retry),
                pendingIntent,
            ).build()
    }

    private val replyNotificationAction by lazy {
        val remoteInput =
            RemoteInput.Builder(REMOTE_INPUT_RESULT_KEY).run {
                setLabel(getString(R.string.pairing_enter_code))
                build()
            }

        val pendingIntent =
            PendingIntent.getForegroundService(
                this,
                REPLY_REQUEST_ID,
                replyIntent(this, -1),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                },
            )

        Notification.Action
            .Builder(
                null,
                getString(R.string.pairing_enter_code),
                pendingIntent,
            ).addRemoteInput(remoteInput)
            .build()
    }

    private fun replyNotificationAction(port: Int): Notification.Action {
        // Ensure pending intent is created
        val action = replyNotificationAction

        PendingIntent.getForegroundService(
            this,
            REPLY_REQUEST_ID,
            replyIntent(this, port),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            },
        )

        return action
    }

    private val searchingNotification by lazy {
        Notification
            .Builder(this, NOTIFICATION_CHANNEL)
            .setColor(MaterialColors.getColor(this, android.R.attr.colorPrimary, 0))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(getString(R.string.pairing_searching))
            .addAction(stopNotificationAction)
            .build()
    }

    private fun createInputNotification(port: Int): Notification =
        Notification
            .Builder(this, NOTIFICATION_CHANNEL)
            .setColor(MaterialColors.getColor(this, android.R.attr.colorPrimary, 0))
            .setContentTitle(getString(R.string.pairing_service_found))
            .setSmallIcon(R.drawable.ic_system_icon)
            .addAction(replyNotificationAction(port))
            .build()

    private val workingNotification by lazy {
        Notification
            .Builder(this, NOTIFICATION_CHANNEL)
            .setColor(MaterialColors.getColor(this, android.R.attr.colorPrimary, 0))
            .setContentTitle(getString(R.string.pairing_in_progress))
            .setSmallIcon(R.drawable.ic_system_icon)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
