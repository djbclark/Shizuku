package moe.shizuku.manager.core.ui.helpers

import android.content.Context
import androidx.annotation.StringRes

sealed interface UiText {
    data class StringLiteral(val value: String) : UiText
    data class StringResource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    fun asString(context: Context): String {
        return when (this) {
            is StringLiteral -> value
            is StringResource -> context.getString(resId, *args.toTypedArray())
        }
    }
}