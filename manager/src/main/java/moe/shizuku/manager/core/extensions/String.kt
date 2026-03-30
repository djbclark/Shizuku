package moe.shizuku.manager.core.extensions

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
