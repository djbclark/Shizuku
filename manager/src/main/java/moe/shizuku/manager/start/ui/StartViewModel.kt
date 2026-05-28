package moe.shizuku.manager.start.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.fold
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.adb.models.AdbConnectionError
import moe.shizuku.manager.core.platform.adb.models.AdbPortError
import moe.shizuku.manager.core.platform.adb.models.AdbSettingsError
import moe.shizuku.manager.core.ui.helpers.UiText
import moe.shizuku.manager.core.utils.root.RootError
import moe.shizuku.manager.core.utils.runnable.RunnableStatus
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.start.StartStep
import moe.shizuku.manager.start.models.StartStepItem
import moe.shizuku.manager.start.models.StartUiState
import moe.shizuku.manager.start.models.StartStepUiStatus

class StartViewModel(
    private val privilegedServiceManager: PrivilegedServiceManager
) : ViewModel() {

    private val session = privilegedServiceManager.createStartSession()

    val uiState: StateFlow<StartUiState> = combine(
        session.steps,
        session.status
    ) { steps, status ->
        StartUiState(
            steps = steps.map { step ->
                StartStepItem(
                    label = mapStepLabel(step),
                    icon = mapStepIcon(step),
                    status = mapStepStatus(step)
                )
            },
            status = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StartUiState()
    )

    init {
        viewModelScope.launch {
            privilegedServiceManager.startService(session)
        }
    }

    private fun mapStepLabel(step: StartStep<*, *>): Int {
        return when (step) {
            is StartStep.RequestRootPermission -> R.string.start_step_get_root_shell
            is StartStep.EnableUsbDebugging -> R.string.start_step_enabling_debugging
            is StartStep.EnableWirelessDebugging -> R.string.start_step_enabling_wireless_debugging
            is StartStep.CloseTcpPort -> R.string.start_step_closing_tcp_port
            is StartStep.SearchForPort -> R.string.start_step_searching_for_port
            is StartStep.ConnectToPort -> R.string.start_step_connecting_to_port
            is StartStep.OpenTcpPort -> R.string.start_step_opening_tcp_port
            is StartStep.ExecuteCommand -> R.string.start_step_executing_command
            is StartStep.WaitForService -> R.string.start_step_waiting_for_service
        }
    }

    private fun mapStepIcon(step: StartStep<*, *>): Int {
        return when (step) {
            is StartStep.RequestRootPermission -> R.drawable.ic_system_icon
            is StartStep.EnableUsbDebugging -> R.drawable.ic_outline_notifications_active_24
            is StartStep.EnableWirelessDebugging -> R.drawable.ic_outline_notifications_active_24
            is StartStep.CloseTcpPort -> R.drawable.ic_close_24
            is StartStep.SearchForPort -> R.drawable.ic_code_24dp
            is StartStep.ConnectToPort -> R.drawable.ic_baseline_link_24
            is StartStep.OpenTcpPort -> R.drawable.ic_outline_play_arrow_24
            is StartStep.ExecuteCommand -> R.drawable.ic_terminal_24
            is StartStep.WaitForService -> R.drawable.ic_server_start_24dp
        }
    }

    private fun mapStepStatus(step: StartStep<*, *>): StartStepUiStatus {
        return when (step) {
            is StartStep.RequestRootPermission -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { reason ->
                    val uiText = when (reason) {
                        RootError.PermissionDenied -> UiText.StringLiteral("Root permission was denied")
                    }
                    StartStepUiStatus.Failed(uiText)
                }
            )

            is StartStep.EnableUsbDebugging -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { reason ->
                    val uiText = when (reason) {
                        AdbSettingsError.NoWifi -> UiText.StringResource(R.string.start_error_wifi_required)
                        AdbSettingsError.NoWriteSecureSettings -> UiText.StringResource(
                            R.string.start_error_write_secure_settings,
                            listOf("WRITE_SECURE_SETTINGS")
                        )
                        AdbSettingsError.NotAuthorized -> UiText.StringResource(R.string.start_error_authorization_required)
                        AdbSettingsError.NotSupported -> UiText.StringResource(R.string.start_error_tls_not_supported)
                    }
                    StartStepUiStatus.Failed(uiText)
                }
            )

            is StartStep.EnableWirelessDebugging -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { reason ->
                    val uiText = when (reason) {
                        AdbSettingsError.NoWifi -> UiText.StringResource(R.string.start_error_wifi_required)
                        AdbSettingsError.NoWriteSecureSettings -> UiText.StringResource(
                            R.string.start_error_write_secure_settings,
                            listOf("WRITE_SECURE_SETTINGS")
                        )
                        AdbSettingsError.NotAuthorized -> UiText.StringResource(R.string.start_error_authorization_required)
                        AdbSettingsError.NotSupported -> UiText.StringResource(R.string.start_error_tls_not_supported)
                    }
                    StartStepUiStatus.Failed(uiText)
                }
            )

            is StartStep.SearchForPort -> step.status.value.mapToUiStatus(
                onSuccess = { port ->
                    StartStepUiStatus.Completed(
                        UiText.StringResource(R.string.start_step_port_discovered, listOf(port))
                    )
                },
                onFailure = { reason ->
                    val uiText = when (reason) {
                        AdbPortError.NotFound -> UiText.StringResource(R.string.start_error_port_not_found)
                    }
                    StartStepUiStatus.Failed(uiText)
                }
            )

            is StartStep.ConnectToPort -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { reason ->
                    val uiText = when (reason) {
                        AdbConnectionError.NotPaired -> UiText.StringResource(R.string.start_error_pairing_required)
                        is AdbConnectionError.ConnectionFailed -> {
                            reason.e.message?.let { UiText.StringLiteral(it) }
                                ?: UiText.StringResource(R.string.error)
                        }
                    }
                    StartStepUiStatus.Failed(uiText)
                }
            )

            is StartStep.OpenTcpPort -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { reason ->
                    val uiText = reason?.let { UiText.StringLiteral(it) }
                        ?: UiText.StringResource(R.string.error)
                    StartStepUiStatus.Failed(uiText)
                }
            )

            is StartStep.ExecuteCommand -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { errors ->
                    val message = errors.joinToString("\n")
                    val uiText = if (message.isNotEmpty()) {
                        UiText.StringLiteral(message)
                    } else {
                        UiText.StringResource(R.string.error)
                    }
                    StartStepUiStatus.Failed(uiText)
                }
            )

            is StartStep.CloseTcpPort -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { StartStepUiStatus.Failed(UiText.StringResource(R.string.error)) }
            )

            is StartStep.WaitForService -> step.status.value.mapToUiStatus(
                onSuccess = { StartStepUiStatus.Completed() },
                onFailure = { StartStepUiStatus.Failed(UiText.StringResource(R.string.error)) }
            )
        }
    }

    private fun <T, E> RunnableStatus<T, E>.mapToUiStatus(
        onSuccess: (T) -> StartStepUiStatus,
        onFailure: (E) -> StartStepUiStatus
    ): StartStepUiStatus {
        return when (this) {
            is RunnableStatus.Pending -> StartStepUiStatus.Pending
            is RunnableStatus.Running -> StartStepUiStatus.Running
            is RunnableStatus.Finished -> result.fold(
                success = onSuccess,
                failure = onFailure
            )
        }
    }
}
