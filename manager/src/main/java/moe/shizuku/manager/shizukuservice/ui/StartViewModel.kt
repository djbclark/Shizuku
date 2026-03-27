package moe.shizuku.manager.shizukuservice.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import moe.shizuku.manager.shizukuservice.ShizukuServiceManager
import rikka.lifecycle.Resource

class StartViewModel(
    private val shizukuServiceManager: ShizukuServiceManager
) : ViewModel() {

    private val sb = StringBuilder()
    private val _output = MutableLiveData<Resource<StringBuilder>>()

    val output = _output as LiveData<Resource<StringBuilder>>

    private val handler =
        CoroutineExceptionHandler { _, throwable ->
            log(error = throwable)
        }

    fun startService() {
        viewModelScope.launch(handler) {
            shizukuServiceManager.startService(log = { log(it) })
        }
    }

    private fun log(
        line: String? = null,
        error: Throwable? = null,
    ) {
        line?.let { sb.appendLine(it) }
        error?.let { sb.appendLine().appendLine(Log.getStackTraceString(it)) }

        if (error == null) {
            _output.postValue(Resource.success(sb))
        } else {
            _output.postValue(Resource.error(error, sb))
        }
    }
}
