package moe.shizuku.manager.management

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.databinding.AuthorizedAppsFragmentBinding
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import java.util.Objects

class AuthorizedAppsFragment : Fragment() {

    private val viewModel: AppsViewModel by viewModels()
    private val adapter = AppsAdapter()

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isDead() && !isStateSaved) {
            findNavController().popBackStack()
        }
    }

    private var _binding: AuthorizedAppsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AuthorizedAppsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!ShizukuStateMachine.isRunning()) {
            findNavController().popBackStack()
            return
        }

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.packages.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    adapter.updateData(it.data)
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
        recyclerView.fixEdgeEffect()
        recyclerView.addEdgeSpacing(top = 8f, bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                viewModel.load(true)
            }
        })

        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onDestroyView() {
        ShizukuStateMachine.removeListener(stateListener)
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
