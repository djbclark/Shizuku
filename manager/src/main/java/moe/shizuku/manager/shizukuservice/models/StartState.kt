package moe.shizuku.manager.shizukuservice.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import moe.shizuku.manager.R

sealed class StepState {
    object Pending : StepState()
    object Running : StepState()
    object Completed : StepState()
    data class Failed(val throwable: Throwable) : StepState()
}

sealed class StartStep(
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    var state: StepState = StepState.Pending
) {
    object RequestingRoot : StartStep(R.string.start_step_requesting_root, R.drawable.ic_system_icon)
    object ClosingTcpPort : StartStep(R.string.start_step_closing_tcp_port, R.drawable.ic_close_24)
    object SearchingForPort : StartStep(R.string.start_step_searching_for_port, R.drawable.ic_code_24dp)
    object AwaitingAuthorization : StartStep(R.string.start_step_awaiting_authorization, R.drawable.ic_outline_notifications_active_24)
    object ConnectingToPort : StartStep(R.string.start_step_connecting_to_port, R.drawable.ic_baseline_link_24)
    object OpeningTcpPort : StartStep(R.string.start_step_opening_tcp_port, R.drawable.ic_outline_play_arrow_24)
    object ExecutingCommand : StartStep(R.string.start_step_executing_command, R.drawable.ic_terminal_24)
    object WaitingForService : StartStep(R.string.start_step_waiting_for_service, R.drawable.ic_server_start_24dp)

    fun reset() {
        state = StepState.Pending
    }
}
