package moe.shizuku.manager.core.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.setNavBarScrim
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.StyledBottomSheetBinding

abstract class StyledBottomSheet : BottomSheetDialogFragment(R.layout.styled_bottom_sheet) {

    protected var titleRes: Int?
        @StringRes get() = arguments?.getInt("arg_title")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_title", value ?: 0)
        }

    protected var footerRes: Int?
        @StringRes get() = arguments?.getInt("arg_footer")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_footer", value ?: 0)
        }

    abstract fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?): View

    private val binding by viewBinding(StyledBottomSheetBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            val contentView = onCreateContentView(layoutInflater, contentContainer)

            header.isVisible = titleRes != null
            titleRes?.let {
                title.setText(it)
                buttonClose.setOnClickListener { dismiss() }
            }

            contentContainer.addView(contentView)

            footer.isVisible = footerRes != null
            footerRes?.let {
                footerText.setText(it)
            }

            dialog?.window?.enableEdgeToEdge()
        }
    }

    // We must handle edge-to-edge manually
    // The default implementation doesn't work properly
    private fun Window.enableEdgeToEdge() {
        if (binding.footer.isVisible) {
            // Apply insets to the whole bottom sheet to move the footer above the nav bar
            binding.root.applySystemBarsPadding(bottom = true)

            // Also use a transparent nav bar since content won't scroll under the footer
            setNavBarScrim(false)
        } else {
            val contentView = binding.contentContainer.getChildAt(0)
            with(contentView) {
                // Apply insets to the content so that it fully scrolls above the nav bar
                applySystemBarsPadding(bottom = true)

                // Apply a scrim to the nav bar only if the content is scrollable
                val updateNavigationScrim = ViewTreeObserver.OnScrollChangedListener {
                    val canScroll = canScrollVertically(1) || canScrollVertically(-1)
                    setNavBarScrim(canScroll)
                }
                viewTreeObserver.addOnScrollChangedListener(updateNavigationScrim)
                post { updateNavigationScrim.onScrollChanged() }
            }
        }
    }
}
