package moe.shizuku.manager.privilegedservice.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.core.utils.runnable.RunnableStatus
import moe.shizuku.manager.databinding.StartFragmentBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class StartFragment : Fragment(R.layout.start_fragment) {
    private val viewModel: StartViewModel by viewModel()
    private val binding by viewBinding(StartFragmentBinding::bind)
    private val adapter = StartStepAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { sequence ->
                    if (sequence == null) return@collectLatest

                    launch {
                        sequence.steps.collect { steps ->
                            adapter.submitList(steps)
                            adapter.notifyDataSetChanged()
                        }
                    }

                    launch {
                        sequence.status.collect { status ->
                            if (status is RunnableStatus.Completed) {
                                delay(3000)
                                if (isResumed) findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }

        viewModel.startService()
    }
}
