package moe.shizuku.manager.permission.ui.authorizedapps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.core.utils.AppIconCache
import moe.shizuku.manager.core.utils.ShizukuSystemApis
import moe.shizuku.manager.core.utils.UserHandleCompat
import moe.shizuku.manager.databinding.AuthorizedAppsFragmentBinding
import moe.shizuku.manager.permission.PermissionManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.lifecycle.Status
import java.util.Objects

class AuthorizedAppsFragment : Fragment(R.layout.authorized_apps_fragment) {

    private val viewModel: AuthorizedAppsViewModel by viewModel()
    private val permissionManager: PermissionManager by inject()
    private val shizukuSystemApis: ShizukuSystemApis by inject()
    private val userHandleCompat: UserHandleCompat by inject()
    private val appIconCache: AppIconCache by inject()
    private val adapter by lazy {
        AppsAdapter(
            permissionManager,
            shizukuSystemApis,
            userHandleCompat,
            appIconCache
        )
    }

    private val binding by viewBinding(AuthorizedAppsFragmentBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.packages.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.let { list ->
                        adapter.updateData(list)
                    }
                }

                Status.ERROR -> {
                    if (isAdded) {
                        findNavController().popBackStack()
                        val tr = it.error
                        toast(Objects.toString(tr, "unknown"))
                        tr.printStackTrace()
                    }
                }

                Status.LOADING -> {

                }
            }
        }
        viewModel.load()

        val recyclerView = binding.list
        recyclerView.adapter = adapter

        recyclerView.applySystemBarsPadding(bottom = true)

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                viewModel.load(true)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
