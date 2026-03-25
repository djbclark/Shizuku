package moe.shizuku.manager.core.utils

import android.content.pm.PackageManager
import android.os.RemoteException
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.hidden.compat.PermissionManagerApis
import rikka.hidden.compat.UserManagerApis
import rikka.hidden.compat.util.SystemServiceBinder
import rikka.shizuku.ShizukuBinderWrapper

class ShizukuSystemApis(
    private val stateMachine: ShizukuStateMachine,
    private val userHandleCompat: UserHandleCompat
) {
    data class UserInfoCompat(
        val id: Int,
        val name: String?
    )

    init {
        SystemServiceBinder.setOnGetBinderListener {
            return@setOnGetBinderListener ShizukuBinderWrapper(it)
        }
    }

    private val users = arrayListOf<UserInfoCompat>()

    private fun getUsers(): List<UserInfoCompat> {
        return if (!stateMachine.isRunning()) {
            arrayListOf(UserInfoCompat(userHandleCompat.myUserId(), "Owner"))
        } else try {
            val list = UserManagerApis.getUsers(true, true, true)
            val users: MutableList<UserInfoCompat> = ArrayList()
            for (ui in list) {
                users.add(UserInfoCompat(ui.id, ui.name))
            }
            return users
        } catch (_: Throwable) {
            arrayListOf(UserInfoCompat(userHandleCompat.myUserId(), "Owner"))
        }
    }

    fun getUsers(useCache: Boolean = true): List<UserInfoCompat> {
        synchronized(users) {
            if (!useCache || users.isEmpty()) {
                users.clear()
                users.addAll(getUsers())
            }
            return users
        }
    }

    fun getUserInfo(userId: Int): UserInfoCompat {
        return getUsers(useCache = true).firstOrNull { it.id == userId } ?: UserInfoCompat(
            userHandleCompat.myUserId(),
            "Unknown"
        )
    }

    fun checkPermission(permName: String, pkgName: String, userId: Int): Int {
        return if (!stateMachine.isRunning()) {
            PackageManager.PERMISSION_DENIED
        } else try {
            PermissionManagerApis.checkPermission(permName, pkgName, userId)
        } catch (tr: RemoteException) {
            throw RuntimeException(tr.message, tr)
        }
    }
}
