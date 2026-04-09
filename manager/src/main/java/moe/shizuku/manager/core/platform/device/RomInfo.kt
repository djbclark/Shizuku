package moe.shizuku.manager.core.platform.device

import android.os.Build
import android.os.SystemProperties

@Suppress("SpellCheckingInspection", "unused")
object RomInfo {
    val isMiui: Boolean by lazy {
        isManufacturer(setOf("Xiaomi", "POCO", "Redmi")) ||
                hasProperty("ro.miui.ui.version.name")
    }

    val isHyperOs: Boolean by lazy {
        isManufacturer(setOf("Xiaomi", "POCO", "Redmi")) ||
                hasProperty("ro.mi.os.version.name")
    }

    val isColorOs: Boolean by lazy {
        isManufacturer(setOf("OnePlus", "Oppo")) ||
                hasProperty("ro.build.version.opporom")
    }

    val isFlyme: Boolean by lazy {
        isManufacturer(setOf("Meizu")) ||
                hasProperty("ro.build.flyme.version")
    }

    val isEmui: Boolean by lazy {
        isManufacturer(setOf("Huawei")) ||
                hasProperty("ro.build.version.emui")
    }

    private fun isManufacturer(manufacturer: Set<String>): Boolean =
        manufacturer.any {
            Build.MANUFACTURER.contains(it, ignoreCase = true) ||
                    Build.BRAND.contains(it, ignoreCase = true)
        }

    private fun hasProperty(property: String): Boolean =
        SystemProperties.get(property).isNotEmpty()
}