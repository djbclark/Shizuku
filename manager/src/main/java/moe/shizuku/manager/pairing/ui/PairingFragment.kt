package moe.shizuku.manager.pairing.ui

import android.app.ForegroundServiceStartNotAllowedException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.platform.device.RomInfo
import moe.shizuku.manager.core.platform.settings.SettingsIntentFactory
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.PairingFragmentBinding
import moe.shizuku.manager.pairing.notifications.AdbPairingNotification
import moe.shizuku.manager.pairing.services.AdbPairingService
import org.koin.android.ext.android.inject

@RequiresApi(Build.VERSION_CODES.R)
class PairingFragment : Fragment(R.layout.pairing_fragment) {
    private val binding by viewBinding(PairingFragmentBinding::bind)
    private val notificationProvider: AdbPairingNotification by inject()
    private val settingsIntentFactory: SettingsIntentFactory by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        binding.apply {
            miui.isVisible = RomInfo.isMiui

            developerOptions.setOnClickListener {
                startActivity(settingsIntentFactory.wirelessDebugging())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (notificationProvider.isChannelEnabled()) startPairingService()
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
                // TODO
            }
        }
    }
}