package moe.shizuku.manager.core.data.preferences

import moe.shizuku.manager.core.data.KeyValueEntry

@Suppress("RemoveExplicitTypeArguments")
object PreferenceKeys {
    // -------------------------
    // BEHAVIOR
    // -------------------------

    val START_MODE =
        KeyValueEntry<StartMode>(
            key = "start_mode",
            default = StartMode.PC,
        )

    val START_ON_BOOT =
        KeyValueEntry<Boolean>(
            key = "start_on_boot",
            default = false,
        )

    val WATCHDOG =
        KeyValueEntry<Boolean>(
            key = "watchdog",
            default = false,
        )

    val TCP_MODE =
        KeyValueEntry<Boolean>(
            key = "tcp_mode",
            default = true,
        )

    val TCP_PORT =
        KeyValueEntry<Int>(
            key = "tcp_port",
            default = 5555,
        )

    val AUTO_DISABLE_USB_DEBUGGING =
        KeyValueEntry<Boolean>(
            key = "auto_disable_usb_debugging",
            default = false,
        )

    // -------------------------
    // APPEARANCE
    // -------------------------

    val LANGUAGE =
        KeyValueEntry<String?>(
            key = "language",
            default = null,
        )

    val THEME =
        KeyValueEntry<Theme>(
            key = "theme",
            default = Theme.SYSTEM,
        )

    val AMOLED_BLACK =
        KeyValueEntry<Boolean>(
            key = "amoled_black",
            default = false,
        )

    val DYNAMIC_COLOR =
        KeyValueEntry<Boolean>(
            key = "dynamic_color",
            default = true,
        )

    // -------------------------
    // UPDATES
    // -------------------------

    val CHECK_FOR_UPDATES =
        KeyValueEntry<Boolean>(
            key = "check_for_updates",
            default = true,
        )

    val UPDATE_CHANNEL =
        KeyValueEntry<UpdateChannel>(
            key = "update_channel",
            default = UpdateChannel.STABLE,
        )

    // -------------------------
    // ADVANCED
    // -------------------------

    val LEGACY_PAIRING =
        KeyValueEntry<Boolean>(
            key = "legacy_pairing",
            default = false,
        )
}
