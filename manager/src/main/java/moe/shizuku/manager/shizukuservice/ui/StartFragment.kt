package moe.shizuku.manager.shizukuservice.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.core.adb.AdbKeyException
import moe.shizuku.manager.databinding.StartFragmentBinding
import moe.shizuku.manager.shizukuservice.models.NotRootedException
import moe.shizuku.manager.starter.Starter
import rikka.lifecycle.Status
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLProtocolException

class StartFragment : Fragment() {
    private val startViewModel: StartViewModel by viewModels()

    private var _binding: StartFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StartFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_24)
        }

        startViewModel.output.observe(viewLifecycleOwner) {
            val output = it.data!!.trim()
            if (output.endsWith(Starter.serviceStartedMessage)) {
                requireActivity().window?.decorView?.postDelayed({
                    if (isAdded) findNavController().popBackStack()
                }, 3000)
            } else if (it.status == Status.ERROR) {
                val message = when (it.error) {
                    is AdbKeyException -> R.string.adb_error_key_store
                    is NotRootedException -> R.string.start_error_root
                    is SocketTimeoutException, is ConnectException -> R.string.start_error_connection
                    is SSLProtocolException -> R.string.start_error_pairing_required
                    else -> 0
                }

                if (message != 0) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            binding.text1.text = output
        }

        val port = arguments?.getInt("port", 0) ?: 0
        val isRoot = arguments?.getBoolean("root", false) ?: false

        startViewModel.start(isRoot, port)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val EXTRA_IS_ROOT = "moe.shizuku.manager.extra.IS_ROOT"
        const val EXTRA_PORT = "moe.shizuku.manager.extra.PORT"
    }
}
