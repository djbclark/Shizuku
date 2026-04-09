package moe.shizuku.manager.core.extensions

import android.os.LocaleList
import java.util.Locale

fun LocaleList.toList(): List<Locale> {
    return List(size()) { i -> get(i) }
}