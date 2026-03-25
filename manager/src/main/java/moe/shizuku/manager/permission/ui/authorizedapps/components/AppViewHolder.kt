package moe.shizuku.manager.permission.ui.authorizedapps.components

import android.content.pm.PackageInfo
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import moe.shizuku.manager.R
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.utils.AppIconCache
import moe.shizuku.manager.core.utils.ShizukuSystemApis
import moe.shizuku.manager.core.utils.UserHandleCompat
import moe.shizuku.manager.databinding.AppListItemBinding
import rikka.shizuku.Shizuku

class AppViewHolder(
    private val binding: AppListItemBinding,
    private val permissionManager: PermissionManager,
    private val shizukuSystemApis: ShizukuSystemApis,
    private val userHandleCompat: UserHandleCompat,
    private val appIconCache: AppIconCache,
    private val onAuthorizationsChanged: () -> Unit
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    private val icon get() = binding.icon
    private val name get() = binding.title
    private val pkg get() = binding.summary
    private val switchWidget get() = binding.switchWidget
    private val root get() = binding.requiresRoot

    private var _data: PackageInfo? = null
    private val data: PackageInfo get() = _data!!

    init {
        itemView.filterTouchesWhenObscured = true
        itemView.setOnClickListener(this)
        itemView.applySystemBarsPadding(start = true, end = true)
    }

    private inline val ai get() = data.applicationInfo!!
    private inline val uid get() = ai.uid

    private var loadIconJob: Job? = null

    override fun onClick(v: View) {
        val context = v.context
        try {
            if (permissionManager.granted(uid)) {
                permissionManager.revoke(uid)
            } else {
                permissionManager.grant(uid)
            }
        } catch (_: SecurityException) {
            val uid = try {
                Shizuku.getUid()
            } catch (_: Throwable) {
                return
            }
            if (uid != 0) {
                val dialog = MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.status_adb_restricted)
                    .setMessage(
                        context.getString(
                            R.string.status_adb_restricted_message,
                            "PLACEHOLDER",
                        )
                    ).setPositiveButton(android.R.string.ok, null)
                    .create()
                dialog.setOnShowListener {
                    (it as AlertDialog).findViewById<TextView>(android.R.id.message)?.movementMethod =
                        LinkMovementMethod.getInstance()
                }
                try {
                    dialog.show()
                } catch (_: Throwable) {
                }
            }
        }
        onAuthorizationsChanged()
    }

    fun bind(data: PackageInfo) {
        this._data = data
        val pm = itemView.context.packageManager
        val userId = userHandleCompat.getUserId(uid)
        icon.setImageDrawable(ai.loadIcon(pm))
        name.text = if (userId != userHandleCompat.myUserId()) {
            val userInfo = shizukuSystemApis.getUserInfo(userId)
            "${ai.loadLabel(pm)} - ${userInfo.name} ($userId)"
        } else {
            ai.loadLabel(pm)
        }
        pkg.text = ai.packageName
        updateCheckedState()
        root.visibility = if (ai.metaData != null && ai.metaData.getBoolean("moe.shizuku.client.V3_REQUIRES_ROOT")) View.VISIBLE else View.GONE

        loadIconJob?.cancel()
        loadIconJob = appIconCache.loadIconBitmapAsync(itemView.context, ai, ai.uid / 100000, icon)
    }

    fun updateCheckedState() {
        switchWidget.isChecked = permissionManager.granted(uid)
    }

    fun recycle() {
        loadIconJob?.cancel()
        loadIconJob = null
    }
}
