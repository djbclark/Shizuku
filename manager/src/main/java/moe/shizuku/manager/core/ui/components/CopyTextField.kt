package moe.shizuku.manager.core.ui.components

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import moe.shizuku.manager.core.extensions.copyToClipboard
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.CopyTextFieldBinding

class CopyTextField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding by viewBinding(CopyTextFieldBinding::inflate)

    var text: CharSequence?
        get() = binding.text.text
        set(value) {
            binding.text.text = value
        }

    init {
        binding.text.movementMethod = ScrollingMovementMethod()
        binding.buttonCopy.setOnClickListener {
            context.copyToClipboard(text?.toString().orEmpty())
        }
    }
}
