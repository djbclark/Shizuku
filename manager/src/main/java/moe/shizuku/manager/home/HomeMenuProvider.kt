package moe.shizuku.manager.home

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.findNavController
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionBottomSheet
import moe.shizuku.manager.core.utils.AppIconCache
import moe.shizuku.manager.home.components.AboutDialog
import moe.shizuku.manager.home.models.HelpItem
import moe.shizuku.manager.updater.UpdateHelper

class HomeMenuProvider(
    private val fragment: HomeFragment,
    private val updateHelper: UpdateHelper,
    private val appIconCache: AppIconCache
) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_about -> {
                AboutDialog(
                    fragment.requireActivity(),
                    updateHelper,
                    appIconCache
                ).show()
                true
            }

            R.id.action_settings -> {
                fragment.findNavController().navigate(R.id.navigate_to_settings)
                true
            }

            R.id.action_help -> {
                showHelpSelector()
                true
            }

            else -> false
        }
    }

    private fun showHelpSelector() {
        ListSelectionBottomSheet.show(
            fragment.childFragmentManager,
            title = R.string.help_and_feedback,
            items = HelpItem.entries
        )
    }
}
