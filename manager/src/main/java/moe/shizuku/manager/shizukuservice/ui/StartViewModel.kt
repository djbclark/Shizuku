package moe.shizuku.manager.shizukuservice.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import moe.shizuku.manager.shizukuservice.starter.AdbStarter
import moe.shizuku.manager.shizukuservice.models.NotRootedException
import moe.shizuku.manager.starter.Starter
import rikka.lifecycle.Resource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class StartViewModel(
    private val adbStarter: AdbStarter,
    private val starter: Starter
) : ViewModel() {

    private val sb = StringBuilder()
    private val _output = MutableLiveData<Resource<StringBuilder>>()

    val output = _output as LiveData<Resource<StringBuilder>>

    private val handler =
        CoroutineExceptionHandler { _, throwable ->
            log(error = throwable)
        }

    private var started = false

    fun start(
        root: Boolean,
        port: Int,
    ) {
        if (started) return
        started = true

        viewModelScope.launch(handler) {
            if (root) {
                startRoot()
            } else {
                adbStarter.startAdb(port) { log(it) }
            }
            starter.waitForBinder { log(it) }
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

    private suspend fun startRoot() {
        log("Starting with root...\n")

        return withContext(Dispatchers.IO) {
            if (!Shell.getShell().isRoot) {
                Shell.getCachedShell()?.close()

                if (!Shell.getShell().isRoot) {
                    Shell.getCachedShell()?.close()
                    throw NotRootedException()
                }
            }

            suspendCancellableCoroutine { cont ->
                Shell
                    .cmd(starter.internalCommand)
                    .to(
                        object : CallbackList<String?>() {
                            override fun onAddElement(s: String?) {
                                s?.let { log(it) }
                            }
                        },
                    ).submit {
                        if (it.isSuccess) {
                            cont.resume(Unit)
                        } else {
                            cont.resumeWithException(Exception("Failed to start with root"))
                        }
                    }
            }
        }
    }
}
