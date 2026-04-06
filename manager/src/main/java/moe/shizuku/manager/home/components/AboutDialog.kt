package moe.shizuku.manager.home.components

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.openUrl
import moe.shizuku.manager.core.extensions.setAppIcon
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.databinding.AboutDialogBinding
import moe.shizuku.manager.updater.UpdateHelper
import org.koin.android.ext.android.inject

class AboutDialog : DialogFragment() {
    private val updateHelper: UpdateHelper by inject()
    private val binding by viewBinding(AboutDialogBinding::inflate)


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        binding.apply {
            icon.setAppIcon(context.applicationInfo)

            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName.text = "v${packageInfo.versionName}"

            btnUpdate.setOnClickListener {
                lifecycleScope.launch {
                    updateHelper.checkAndInstallUpdates()
                }
            }

            btnGitHub.setOnClickListener {
                context.openUrl("https://www.github.com/thedjchi/Shizuku")
            }

            btnDonate.setOnClickListener {
                context.openUrl("https://www.buymeacoffee.com/thedjchi")
            }

            developer.text = getString(
                R.string.about_developer,
                getString(R.string.about_developer_name)
            )

            fork.text = getString(
                R.string.about_fork,
                getString(R.string.about_fork_developer_name)
            )
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .create()

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        return dialog
    }
}