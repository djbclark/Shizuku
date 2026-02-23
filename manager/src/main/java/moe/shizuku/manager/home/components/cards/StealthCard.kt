package moe.shizuku.manager.home.components.cards

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.navigation.findNavController
import moe.shizuku.manager.R

class StealthCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BaseCard(context, attrs) {
    override val cardTitle: String
        get() = context.getString(R.string.stealth_mode)
    override val cardIcon: Int
        get() = R.drawable.ic_visibility_off_outline_24

    override fun onClick(v: View) {
        v.findNavController().navigate(R.id.navigate_to_stealth)
    }
}