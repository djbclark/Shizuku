package moe.shizuku.manager.shizukuservice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.shizukuservice.ShizukuServiceManager

class StartViewModel(
    private val shizukuServiceManager: ShizukuServiceManager
) : ViewModel() {
    val uiState = shizukuServiceManager.startSequence

    fun startService() = viewModelScope.launch {
        shizukuServiceManager.startService()
    }
}
