package moe.shizuku.manager.home.components.cards

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.navigation.findNavController
import moe.shizuku.manager.R

class TerminalCard
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BaseCard(context, attrs) {
    override val cardTitle: String
        get() = context.getString(R.string.terminal_apps)
    override val cardIcon: Int
        get() = R.drawable.ic_terminal_24

    override fun onClick(v: View) {
        v.findNavController().navigate(R.id.navigate_to_terminal_apps)
    }
}