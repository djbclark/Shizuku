package moe.shizuku.manager.privilegedservice.ui.pairing

import android.app.AppOpsManager
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.platform.device.RomInfo
import moe.shizuku.manager.core.platform.settings.SystemSettingsHelper
import moe.shizuku.manager.databinding.PairingFragmentBinding
import moe.shizuku.manager.privilegedservice.services.AdbPairingService

@RequiresApi(Build.VERSION_CODES.R)
class PairingFragment : Fragment() {
    private lateinit var binding: PairingFragmentBinding

    private var notificationEnabled: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PairingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        notificationEnabled = isNotificationEnabled()

        if (notificationEnabled) {
            startPairingService()
        }

        binding.apply {
            miui.isVisible = RomInfo.isMiui

            developerOptions.setOnClickListener {
                SystemSettingsHelper.launchOrHighlightWirelessDebugging(requireContext())
            }
        }
    }

    private fun isNotificationEnabled(): Boolean {
        val nm =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(AdbPairingService.NOTIFICATION_CHANNEL)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }

    override fun onResume() {
        super.onResume()
        startPairingService()
    }

    private fun startPairingService() {
        val intent = AdbPairingService.startIntent(requireContext())
        try {
            requireContext().startForegroundService(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "startForegroundService", e)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                val mode =
                    requireContext().getSystemService(AppOpsManager::class.java)
                        .noteOpNoThrow(
                            "android:start_foreground",
                            Process.myUid(),
                            requireContext().packageName,
                            null,
                            null
                        )
                if (mode == AppOpsManager.MODE_ERRORED) {
                    toast("OP_START_FOREGROUND is denied. What are you doing?")
                }
                requireContext().startService(intent)
            }
        }
    }
}