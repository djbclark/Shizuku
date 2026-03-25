package moe.shizuku.manager.core.ui.components.listselection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.ui.components.StyledBottomSheet
import org.koin.androidx.viewmodel.ext.android.viewModel

class ListSelectionBottomSheet : StyledBottomSheet() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            @StringRes title: Int,
            @StringRes footer: Int? = null,
            items: List<ListSelectionItem>,
            selectedItem: Any? = null
        ) {
            ListSelectionBottomSheet().apply {
                this.titleRes = title
                this.footerRes = footer
                this.items = items
                this.selectedItem = selectedItem
            }.show(fragmentManager, TAG)
        }
    }

    private val viewModel: ListSelectionViewModel by viewModel(ownerProducer = { requireParentFragment() })

    private var items: List<ListSelectionItem>? = null
    private var selectedItem: Any? = null

    private val adapter = ListSelectionAdapter { item ->
        viewModel.select(item)
        dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        items?.let { viewModel.items = it }
        selectedItem?.let { viewModel.selectedItem = it }

        adapter.items = viewModel.items
        adapter.selectedItem = viewModel.selectedItem
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?): View =
        RecyclerView(requireContext()).apply {
            id = android.R.id.list
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@ListSelectionBottomSheet.adapter
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
}
