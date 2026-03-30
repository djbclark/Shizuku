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
    class RequestRoot(action: suspend () -> Unit) : StartStep(
        R.string.start_step_requesting_root, R.drawable.ic_system_icon, action
    )

    class EnableUsbDebugging(action: suspend () -> Unit) : StartStep(
        R.string.start_step_enabling_debugging,
        R.drawable.ic_outline_notifications_active_24,
        action
    )

    class EnableWirelessDebugging(action: suspend () -> Unit) : StartStep(
        R.string.start_step_enabling_wireless_debugging,
        R.drawable.ic_outline_notifications_active_24,
        action
    )

    class CloseTcpPort(action: suspend () -> Unit) : StartStep(
        R.string.start_step_closing_tcp_port, R.drawable.ic_close_24, action
    )

    class SearchForPort(action: suspend () -> Unit) : StartStep(
        R.string.start_step_searching_for_port, R.drawable.ic_code_24dp, action
    )

    class AwaitingAuthorization(action: suspend () -> Unit) : StartStep(
        R.string.start_step_awaiting_authorization,
        R.drawable.ic_outline_notifications_active_24,
        action
    )

    class ConnectToPort(action: suspend () -> Unit) : StartStep(
        R.string.start_step_connecting_to_port, R.drawable.ic_baseline_link_24, action
    )

    class OpenTcpPort(action: suspend () -> Unit) : StartStep(
        R.string.start_step_opening_tcp_port, R.drawable.ic_outline_play_arrow_24, action
    )

    class ExecuteCommand(action: suspend () -> Unit) : StartStep(
        R.string.start_step_executing_command, R.drawable.ic_terminal_24, action
    )

    class WaitForService(action: suspend () -> Unit) : StartStep(
        R.string.start_step_waiting_for_service, R.drawable.ic_server_start_24dp, action
    )
}
