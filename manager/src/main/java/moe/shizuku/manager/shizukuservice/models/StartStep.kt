package moe.shizuku.manager.shizukuservice.models

import androidx.annotation.DrawableRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.utils.step.Step

sealed class StartStep(
    val label: String,
    @param:DrawableRes val icon: Int,
    action: suspend () -> Unit
) : Step(action) {
    class RequestRoot(action: suspend () -> Unit) : StartStep(
        "Request root", R.drawable.ic_system_icon, action
    )

    class CloseTcpPort(action: suspend () -> Unit) : StartStep(
        "Close TCP port", R.drawable.ic_close_24, action
    )

    class SearchForPort(action: suspend () -> Unit) : StartStep(
        "Search for port", R.drawable.ic_code_24dp, action
    )

    class AwaitAuthorization(action: suspend () -> Unit) : StartStep(
        "Await authorization", R.drawable.ic_outline_notifications_active_24, action
    )

    class ConnectToPort(action: suspend () -> Unit) : StartStep(
        "Connect to port", R.drawable.ic_baseline_link_24, action
    )

    class OpenTcpPort(action: suspend () -> Unit) : StartStep(
        "Open TCP port", R.drawable.ic_outline_play_arrow_24, action
    )

    class ExecuteCommand(action: suspend () -> Unit) : StartStep(
        "Execute command", R.drawable.ic_terminal_24, action
    )

    class WaitForService(action: suspend () -> Unit) : StartStep(
        "Wait for service", R.drawable.ic_server_start_24dp, action
    )
}