package moe.shizuku.manager.home

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.findNavController
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionBottomSheet
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem
import moe.shizuku.manager.home.components.AboutDialog
import moe.shizuku.manager.home.models.HelpItem

class HomeMenuProvider(private val fragment: HomeFragment) : MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_about -> {
                AboutDialog(fragment.requireActivity()).show()
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
        val items = HelpItem.entries.map {
            ListSelectionItem(
                value = it,
                label = fragment.getString(it.labelRes),
                type = ListSelectionItem.Type.ICON,
                iconRes = R.drawable.ic_outline_open_in_new_24
            )
        }

        ListSelectionBottomSheet.show(
            fragment.childFragmentManager,
            title = R.string.help_and_feedback,
            items = items
        )
    }
}