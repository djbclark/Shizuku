package moe.shizuku.manager.privilegedservice.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager
import moe.shizuku.manager.privilegedservice.models.StartStepItem
import moe.shizuku.manager.privilegedservice.models.StartUiState

class StartViewModel(
    private val privilegedServiceManager: PrivilegedServiceManager
) : ViewModel() {
    private val session = privilegedServiceManager.createStartSession()
    val uiState = combine(
        session.steps,
        session.status
    ) { steps, status ->
        StartUiState(
            steps = steps.map { step ->
                StartStepItem(
                    label = step.label,
                    icon = step.icon,
                    status = step.status.value
                )
            },
            status = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StartUiState()
    )

    fun startService() {
        viewModelScope.launch {
            privilegedServiceManager.startService(session)
        }
    }
}
