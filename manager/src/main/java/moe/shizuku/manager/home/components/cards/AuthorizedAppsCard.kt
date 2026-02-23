package moe.shizuku.manager.home.components.cards

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.navigation.findNavController
import moe.shizuku.manager.R

class AuthorizedAppsCard
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BaseCard(context, attrs) {
    override val cardTitle: String
        get() = context.getString(R.string.authorized_apps)
    override val cardIcon: Int
        get() = R.drawable.ic_settings_outline_24dp

    fun update(grantedCount: Int) {
        setTitle(
            context.resources.getQuantityString(
                R.plurals.authorized_apps_count,
                grantedCount,
                grantedCount,
            ),
        )
    }

    override fun onClick(v: View) {
        v.findNavController().navigate(R.id.navigate_to_authorized_apps)
    }
}