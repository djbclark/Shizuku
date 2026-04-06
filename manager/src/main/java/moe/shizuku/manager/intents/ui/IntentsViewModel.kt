package moe.shizuku.manager.intents.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import moe.shizuku.manager.intents.data.TokenRepository
import moe.shizuku.manager.intents.models.IntentsUiState

class IntentsViewModel(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _intentAction = MutableStateFlow(IntentsUiState.IntentAction.START)

    val uiState: StateFlow<IntentsUiState> = combine(
        _intentAction,
        tokenRepository.authTokenFlow
    ) { action, token ->
        IntentsUiState(
            enabled = true, // TODO link to shared preferences
            intentAction = action,
            authToken = token
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IntentsUiState(
            enabled = true,
            intentAction = IntentsUiState.IntentAction.START,
            authToken = ""
        )
    )

    fun onIntentActionChanged(action: IntentsUiState.IntentAction) {
        _intentAction.value = action
    }

    fun onRegenerateToken() =
        tokenRepository.regenerateAuthToken()
}
