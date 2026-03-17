package moe.shizuku.manager.core.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.databinding.StyledBottomSheetBinding

abstract class StyledBottomSheet : BottomSheetDialogFragment() {

    var titleRes: Int?
        @StringRes get() = arguments?.getInt("arg_title")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_title", value ?: 0)
        }

    var footerRes: Int?
        @StringRes get() = arguments?.getInt("arg_footer")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_footer", value ?: 0)
        }

    abstract fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?): View

    private var _binding: StyledBottomSheetBinding? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StyledBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            val contentView = onCreateContentView(layoutInflater, contentContainer)

            if (footerRes == null) {
                contentView.applySystemBarsPadding(bottom = true)
            } else {
                root.applySystemBarsPadding(bottom = true)
            }

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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
