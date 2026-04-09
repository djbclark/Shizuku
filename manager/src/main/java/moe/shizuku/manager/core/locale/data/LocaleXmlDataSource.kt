package moe.shizuku.manager.core.locale.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import moe.shizuku.manager.core.extensions.TAG
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class LocaleXmlDataSource {
    fun getLocales(context: Context): List<Locale> {
        val locales = mutableListOf<Locale>()
        try {
            // locales_config.xml is generated at build time
            // Thus, the compiler doesn't have access to R.xml.locales_config
            // So, we must use resources.getIdentifier, which is a discouraged API
            @SuppressLint("DiscouragedApi")
            val resId = context.resources.getIdentifier(
                "_generated_res_locale_config", "xml", context.packageName
            ).takeUnless { it == 0 } ?: run {
                Log.e(TAG, "locales_config.xml not found")
                return emptyList()
            }

            val xpp = context.resources.getXml(resId)
            var eventType = xpp.eventType
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
        } catch (e: Exception) {
            Log.e(TAG, "Error loading locales from XML", e)
        }

        return locales
    }
}