package moe.shizuku.manager.adb

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.navigation.NavDeepLinkBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.extensions.isTelevision
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.platform.adb.AdbSettingsManager
import moe.shizuku.manager.core.platform.adb.client.AdbInvalidPairingCodeException
import moe.shizuku.manager.core.platform.adb.client.AdbKey
import moe.shizuku.manager.core.platform.adb.client.AdbKeyException
import moe.shizuku.manager.core.platform.adb.client.AdbPairingClient
import moe.shizuku.manager.core.platform.adb.client.PreferenceAdbKeyStore
import org.koin.android.ext.android.inject
import java.net.ConnectException

@SuppressLint("AccessibilityPolicy")
class AdbPairingAccessibilityService : AccessibilityService() {
    var port: Int? = null
    var password: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val adbSettingsManager: AdbSettingsManager by inject()
    private val preferencesRepository: PreferencesRepository by inject()

    override fun onServiceConnected() {
        super.onServiceConnected()

        if (!isTelevision && !adbSettingsManager.hasWirelessDebugging) {
            disableSelf()
            return
        }

        navigateToPairing()

        handler.postDelayed({
            toast(R.string.pairing_timed_out)
            disableSelf()
        }, 180_000)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (port != null && password != null) return

        if ((event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == 0) return
        val text = event.source?.text ?: return

        val ipPortRegex = Regex("""(?:\d{1,3}\.){3}\d{1,3}:(\d{2,5})""")
        val passwordRegex = Regex("""\d{6}""")

        ipPortRegex
            .matchEntire(text)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?.let { port = it }
        passwordRegex
            .matchEntire(text)
            ?.value
            ?.let { password = it }

        if (port != null && password != null) {
            val port = port!!
            val password = password!!

            var toastMsg = getString(R.string.pairing_failed)
            GlobalScope.launch(Dispatchers.IO) {
                val context = this@AdbPairingAccessibilityService
                val host = "127.0.0.1"

                val key =
                    try {
                        AdbKey(
                            PreferenceAdbKeyStore(preferencesRepository.prefs),
                            "shizuku"
                        )
                    } catch (_: Throwable) {
                        toastMsg = getString(R.string.adb_error_key_store)
                        return@launch
                    }

                AdbPairingClient(host, port, password, key)
                    .runCatching {
                        start()
                    }.onFailure {
                        when (it) {
                            is ConnectException -> toastMsg =
                                getString(R.string.start_error_connection)

                            is AdbInvalidPairingCodeException -> toastMsg =
                                getString(R.string.pairing_error_invalid_code)

                            is AdbKeyException -> toastMsg = getString(R.string.adb_error_key_store)
                        }
                    }.onSuccess {
                        if (it) {
                            toastMsg =
                                "${
                                    getString(
                                        R.string.pairing_successful,
                                    )
                                }. ${getString(R.string.pairing_successful_message)}"

                            navigateToHome()
                        }
                    }
                withContext(Dispatchers.Main) {
                    context.toast(toastMsg, long = true)
                }
                disableSelf()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }

    private fun navigateToPairing() {
        NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.pairing_fragment)
            .createPendingIntent()
            .send()
    }

    private fun navigateToHome() {
        NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.home_fragment)
            .createPendingIntent()
            .send()
    }
}
