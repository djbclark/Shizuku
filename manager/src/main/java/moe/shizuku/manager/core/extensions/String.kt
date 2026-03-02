package moe.shizuku.manager.core.extensions

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import java.util.Locale

fun String.appendRandomSuffix(n: Int = 5): String {
    require(n > 0) { "The length of the random suffix (n) must be greater than 0." }

    val letters = ('a'..'z')
    val chars = letters + ('0'..'9')

    val first = letters.random()
    val rest = if (n > 1) {
        (2..n).map { chars.random() }
    } else emptyList()

    val randomSuffix = (listOf(first) + rest).joinToString("")

    return "$this.$randomSuffix"
}

fun String.capitalize(locale: Locale): String =
    replaceFirstChar { it.uppercase(locale) }

fun String.asLink(url: String): CharSequence {
    return SpannableString(this).apply {
        setSpan(object : ClickableSpan() {
            override fun onClick(v: View) {
                v.context.openUrl(url)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ds.linkColor
            }
        }, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

fun TextView.applyTemplateArgs(vararg args: CharSequence) {
    val template = text as CharSequence
    text = TextUtils.expandTemplate(template, *args)
    movementMethod = LinkMovementMethod.getInstance()
}