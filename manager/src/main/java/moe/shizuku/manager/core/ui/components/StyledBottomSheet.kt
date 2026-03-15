package moe.shizuku.manager.core.ui.components

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialog
import moe.shizuku.manager.databinding.StyledBottomSheetBinding

abstract class StyledBottomSheet(context: Context) : BottomSheetDialog(context) {
    @get:StringRes
    open val titleRes: Int? = null

    @get:StringRes
    open val footerRes: Int? = null

    abstract val contentView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = StyledBottomSheetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.header.isVisible = titleRes != null
        titleRes?.let {
            binding.title.setText(it)
            binding.buttonClose.setOnClickListener {
                dismiss()
            }
        }

        binding.contentContainer.apply {
            removeAllViews()
            addView(contentView)
        }

        binding.footer.isVisible = footerRes != null
        footerRes?.let {
            binding.footerText.setText(it)
        }

        (context as? LifecycleOwner)?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dismiss()
            }
        })
    }
}
