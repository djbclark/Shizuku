package moe.shizuku.manager.privilegedservice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.privilegedservice.PrivilegedServiceManager

class StartViewModel(
    private val privilegedServiceManager: PrivilegedServiceManager
) : ViewModel() {
    val uiState = privilegedServiceManager.startSequence

    fun startService() = viewModelScope.launch {
        privilegedServiceManager.startService()
    }
}
