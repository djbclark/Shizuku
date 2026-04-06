package moe.shizuku.manager.core.ui.components

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SimpleDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val requestKey = args.getString(ARG_REQUEST_KEY)!!

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.getCharSequence(ARG_TITLE))
            .setMessage(args.getCharSequence(ARG_MESSAGE))
            .setPositiveButton(args.getCharSequence(ARG_POSITIVE_TEXT)) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    requestKey,
                    Bundle().apply { putBoolean(KEY_SUCCESS, true) }
                )
            }
            .setNegativeButton(args.getCharSequence(ARG_NEGATIVE_TEXT)) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    requestKey,
                    Bundle().apply { putBoolean(KEY_SUCCESS, false) }
                )
            }
            .create()
    }

    class Builder(private val context: Context) {
        private var title: CharSequence? = null
        private var message: CharSequence? = null
        private var positiveText: CharSequence? = null
        private var negativeText: CharSequence? = null

        fun setTitle(title: CharSequence?) =
            apply { this.title = title }

        fun setTitle(@StringRes titleId: Int) =
            apply { this.title = context.getText(titleId) }

        fun setMessage(message: CharSequence?) =
            apply { this.message = message }

        fun setMessage(@StringRes messageId: Int) =
            apply { this.message = context.getText(messageId) }

        fun setPositiveButton(@StringRes textId: Int) =
            apply { this.positiveText = context.getString(textId) }

        fun setPositiveButton(text: CharSequence?) =
            apply { this.positiveText = text }

        fun addOkButton() = setPositiveButton(android.R.string.ok)

        fun setNegativeButton(@StringRes textId: Int) =
            apply { this.negativeText = context.getString(textId) }

        fun setNegativeButton(text: CharSequence?) =
            apply { this.negativeText = text }

        fun addCancelButton() = setNegativeButton(android.R.string.cancel)

        fun show(fragmentManager: FragmentManager, key: String) {
            val fragment = SimpleDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_REQUEST_KEY, key)
                    putCharSequence(ARG_TITLE, title)
                    putCharSequence(ARG_MESSAGE, message)
                    putCharSequence(ARG_POSITIVE_TEXT, positiveText)
                    putCharSequence(ARG_NEGATIVE_TEXT, negativeText)
                }
            }
            fragment.show(fragmentManager, key)
        }

        fun <T : Enum<T>> show(fragmentManager: FragmentManager, key: T) =
            show(fragmentManager, key.name)
    }

    companion object {
        private const val ARG_REQUEST_KEY = "request_key"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE_TEXT = "pos_text"
        private const val ARG_NEGATIVE_TEXT = "neg_text"
        const val KEY_SUCCESS = "success"
    }
}

fun Fragment.dialog() = SimpleDialogFragment.Builder(requireContext())
fun FragmentActivity.dialog() = SimpleDialogFragment.Builder(this)

inline fun <reified T : Enum<T>> Fragment.handleDialogResults(
    crossinline onResult: (key: T, success: Boolean) -> Unit
) = enumValues<T>().forEach { enumValue ->
    childFragmentManager.setFragmentResultListener(
        enumValue.name,
        viewLifecycleOwner
    ) { _, bundle ->
        onResult(enumValue, bundle.getBoolean(SimpleDialogFragment.KEY_SUCCESS, false))
    }
}

inline fun <reified T : Enum<T>> FragmentActivity.handleDialogResults(
    crossinline onResult: (key: T, success: Boolean) -> Unit
) = enumValues<T>().forEach { enumValue ->
    supportFragmentManager.setFragmentResultListener(enumValue.name, this) { _, bundle ->
        onResult(enumValue, bundle.getBoolean(SimpleDialogFragment.KEY_SUCCESS, false))
    }
}