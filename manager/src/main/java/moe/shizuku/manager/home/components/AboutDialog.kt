package moe.shizuku.manager.home.components

import android.os.Process
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.openUrl
import moe.shizuku.manager.core.utils.AppIconCache
import moe.shizuku.manager.databinding.AboutDialogBinding
import moe.shizuku.manager.updater.UpdateHelper

class AboutDialog(
    private val activity: ComponentActivity,
    private val updateHelper: UpdateHelper,
    private val appIconCache: AppIconCache
) {

    fun show() {
        val binding = AboutDialogBinding.inflate(
            LayoutInflater.from(activity), null, false
        )

        binding.apply {
            icon.setImageBitmap(
                appIconCache.getOrLoadBitmap(
                    activity,
                    activity.applicationInfo,
                    Process.myUid() / 100000,
                    activity.resources.getDimensionPixelOffset(R.dimen.default_app_icon_size),
                )
            )

            val versionStr = "v${
                activity.packageManager.getPackageInfo(
                    activity.packageName, 0
                ).versionName
            }"

            versionName.text = versionStr

            btnUpdate.setOnClickListener {
                activity.lifecycleScope.launch {
                    updateHelper.checkAndInstallUpdates()
                }
            }

            btnGitHub.setOnClickListener {
                activity.openUrl(
                    "https://www.github.com/thedjchi/Shizuku"
                )
            }

            btnDonate.setOnClickListener {
                activity.openUrl(
                    "https://www.buymeacoffee.com/thedjchi"
                )
            }

            developer.text = activity.getString(
                R.string.about_developer,
                activity.getString(R.string.about_developer_name)
            )

            fork.text = activity.getString(
                R.string.about_fork,
                activity.getString(R.string.about_fork_developer_name)
            )
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}