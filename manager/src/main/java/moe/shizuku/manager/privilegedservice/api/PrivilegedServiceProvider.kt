package moe.shizuku.manager.privilegedservice.api

import android.os.Bundle
import android.util.Log
import androidx.core.os.BundleCompat
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import moe.shizuku.api.BinderContainer
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.utils.ShizukuStateMachine
import org.koin.android.ext.android.inject
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.server.ktx.workerHandler

class PrivilegedServiceProvider : ShizukuProvider() {

    companion object {
        private const val EXTRA_BINDER = "moe.shizuku.privileged.api.intent.extra.BINDER"
        private const val METHOD_SEND_USER_SERVICE = "sendUserService"
    }

    private val stateMachine: ShizukuStateMachine by inject()

    override fun onCreate(): Boolean {
        disableAutomaticSuiInitialization()
        return super.onCreate()
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? =
        if (method == METHOD_SEND_USER_SERVICE && extras != null) handleSendUserService(extras)
        else super.call(method, arg, extras)

    private fun handleSendUserService(extras: Bundle): Bundle? = runCatching {
        extras.classLoader = BinderContainer::class.java.classLoader

        val token = extras.getString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN) ?: return null

        val binder = BundleCompat.getParcelable(
            extras, EXTRA_BINDER, BinderContainer::class.java
        )?.binder ?: return null

        runBlocking(workerHandler.asCoroutineDispatcher()) {
            withTimeout(5000) {
                stateMachine.asFlow().first { it == ShizukuStateMachine.State.RUNNING }

                val serviceArgs =
                    Bundle().apply { putString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN, token) }
                Shizuku.attachUserService(binder, serviceArgs)

                Bundle().apply {
                    putParcelable(EXTRA_BINDER, BinderContainer(Shizuku.getBinder()))
                }
            }
        }
    }.onFailure { e ->
        val msg =
            if (e is TimeoutCancellationException) "Binder not received in 5s" else "Failed to send user service"
        Log.e(TAG, msg, e)
    }.getOrNull()
}