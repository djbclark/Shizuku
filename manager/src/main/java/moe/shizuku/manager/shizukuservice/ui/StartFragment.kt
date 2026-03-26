package moe.shizuku.manager.shizukuservice.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import moe.shizuku.manager.R
import moe.shizuku.manager.core.adb.AdbKeyException
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.showSnackbar
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.databinding.StartFragmentBinding
import moe.shizuku.manager.shizukuservice.models.NotRootedException
import moe.shizuku.manager.starter.Starter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.lifecycle.Status
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLProtocolException

class StartFragment : Fragment(R.layout.start_fragment) {
    private val viewModel: StartViewModel by viewModel()
    private val binding by viewBinding(StartFragmentBinding::bind)
    private val starter: Starter by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        viewModel.output.observe(viewLifecycleOwner) {
            val output = it.data!!.trim()
            if (output.endsWith(starter.serviceStartedMessage)) {
                requireView().postDelayed({
                    findNavController().popBackStack()
                }, 3000)
            } else if (it.status == Status.ERROR) {
                val message = when (it.error) {
                    is AdbKeyException -> R.string.adb_error_key_store
                    is NotRootedException -> R.string.start_error_root
                    is SocketTimeoutException, is ConnectException -> R.string.start_error_connection
                    is SSLProtocolException -> R.string.start_error_pairing_required
                    else -> null
                }

                message?.let { msg -> showSnackbar(msg) }
            }
            binding.text1.text = output
        }

        viewModel.startService(requireContext())
    }
}
