package moe.shizuku.manager.core.locale.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.Log
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.resultOf
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class LocaleXmlDataSource(
    private val context: Context
) {
    fun getLocales(): List<Locale>? {
        // locales_config.xml is generated at build time
        // Thus, the compiler doesn't have access to locale config file
        // So, we must use resources.getIdentifier, which is a discouraged API
        @SuppressLint("DiscouragedApi")
        val resId = context.resources.getIdentifier(
            "_generated_res_locale_config", "xml", context.packageName
        )
        val xpp = context.resources.getXml(resId)

        return parseLocaleConfig(xpp)
    }

    private fun parseLocaleConfig(xpp: XmlResourceParser): List<Locale>? = resultOf {
        var eventType = xpp.eventType
        val locales = mutableListOf<Locale>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xpp.name == "locale") {
                val name = xpp.getAttributeValue(
                    "http://schemas.android.com/apk/res/android", "name"
                )
                if (name != null) {
                    locales.add(Locale.forLanguageTag(name))
                }
            }
            eventType = xpp.next()
        }

        locales.toList()
    }.getOrNull()
}