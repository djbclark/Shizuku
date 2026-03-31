package moe.shizuku.manager.privilegedservice.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.utils.runnable.Runnable

sealed class StartStep(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    action: suspend () -> Unit
) : Runnable(action) {
    data class RequestRoot(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_requesting_root, R.drawable.ic_system_icon, action
    )

    data class EnableUsbDebugging(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_enabling_debugging,
        R.drawable.ic_outline_notifications_active_24,
        action
    )

    data class EnableWirelessDebugging(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_enabling_wireless_debugging,
        R.drawable.ic_outline_notifications_active_24,
        action
    )

    data class CloseTcpPort(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_closing_tcp_port, R.drawable.ic_close_24, action
    )

    data class SearchForPort(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_searching_for_port, R.drawable.ic_code_24dp, action
    )

    data class AwaitingAuthorization(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_awaiting_authorization,
        R.drawable.ic_outline_notifications_active_24,
        action
    )

    data class ConnectToPort(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_connecting_to_port, R.drawable.ic_baseline_link_24, action
    )

    data class OpenTcpPort(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_opening_tcp_port, R.drawable.ic_outline_play_arrow_24, action
    )

    data class ExecuteCommand(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_executing_command, R.drawable.ic_terminal_24, action
    )

    data class WaitForService(private val action: suspend () -> Unit) : StartStep(
        R.string.start_step_waiting_for_service, R.drawable.ic_server_start_24dp, action
    )
}
