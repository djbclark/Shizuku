package moe.shizuku.manager.pairing.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.data.PreferencesRepository
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.core.platform.adb.client.PairingCodeException
import moe.shizuku.manager.core.platform.adb.client.AdbKey
import moe.shizuku.manager.core.platform.adb.AdbMdns
import moe.shizuku.manager.core.platform.adb.client.AdbPairingClient
import moe.shizuku.manager.core.platform.adb.client.PreferenceAdbKeyStore
import moe.shizuku.manager.core.platform.settings.SettingsIntentFactory
import moe.shizuku.manager.databinding.AdbPairDialogBinding
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.net.ConnectException

@RequiresApi(VERSION_CODES.R)
class AdbPairDialogFragment : DialogFragment() {
    private val viewModel: AdbPairingViewModel by activityViewModel()
    private val settingsIntentFactory: SettingsIntentFactory by inject()
    private val binding by viewBinding(AdbPairDialogBinding::inflate)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder =
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(R.string.pairing_searching)
                setView(binding.root)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(android.R.string.ok, null)
                setNeutralButton(R.string.developer_options, null)
            }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnShowListener { onDialogShow(dialog) }
        return dialog
    }

    private fun onDialogShow(dialog: AlertDialog) {
        binding.text1.text =
            buildString {
                append(getString(R.string.pairing_steps_intro))
                append("\n -")
                append(getString(R.string.pairing_tutorial_1))
                append("\n -")
                append(getString(R.string.pairing_tutorial_2))
                append("\n -")
                append(getString(R.string.pairing_tutorial_3))
            }

        binding.pairingCode.editText!!.doAfterTextChanged {
            binding.pairingCode.error = null
        }

        binding.pairingCode.error = null

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = false

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            startActivity(settingsIntentFactory.wirelessDebugging())
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val port =
                try {
                    binding.port.editText!!
                        .text
                        .toString()
                        .toInt()
                } catch (_: Exception) {
                    -1
                }

            val password =
                binding.pairingCode.editText!!
                    .text
                    .toString()

            viewModel.run(port, password)
        }

        viewModel.port.observe(this) {
            if (it == -1) {
                dialog.setTitle(R.string.pairing_searching)
                binding.text1.isVisible = true
                binding.pairingCode.isVisible = false
                binding.port.editText!!.setText(it.toString())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = false
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isVisible = true
            } else {
                dialog.setTitle(R.string.pairing_service_found)
                binding.text1.isVisible = false
                binding.pairingCode.isVisible = true
                binding.port.editText!!.setText(it.toString())
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isVisible = true
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).isVisible = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()
        val inMultiScreenOrDisplay = (
                requireActivity().isInMultiWindowMode ||
                        (
                                requireActivity()
                                    .window
                                    ?.decorView
                                    ?.display
                                    ?.displayId ?: -1
                                ) > 0
                )

        binding.text1.isVisible = inMultiScreenOrDisplay
        binding.text2.isVisible = !inMultiScreenOrDisplay

        if (inMultiScreenOrDisplay) {
            dialog?.setTitle(R.string.pairing_searching)
        } else {
            dialog?.setTitle(R.string.pair)
        }

        viewModel.result.observe(viewLifecycleOwner) {
            if (it == null) {
                dismissAllowingStateLoss()
            } else {
                when (it) {
                    is ConnectException -> {
                        binding.port.error = context.getString(R.string.start_error_connection)
                    }

                    is PairingCodeException -> {
                        binding.pairingCode.error =
                            context.getString(R.string.pairing_error_invalid_code)
                    }
                }
            }
        }
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }

    override fun getDialog(): AlertDialog? = super.getDialog() as AlertDialog?
}

@SuppressLint("NewApi")
class AdbPairingViewModel(
    adbMdns: AdbMdns,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _result = MutableLiveData<Throwable?>()
    val result: LiveData<Throwable?> = _result as LiveData<Throwable?>

    private val _port = MutableLiveData<Int>()
    val port: LiveData<Int> = _port as LiveData<Int>

    init {
        adbMdns.pairingFlow
            .onEach { _port.postValue(it) }
            .launchIn(viewModelScope)
    }

    fun run(
        port: Int,
        password: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val host = "127.0.0.1"
            val key = AdbKey(PreferenceAdbKeyStore(preferencesRepository.prefs), "shizuku")

            AdbPairingClient(host, port, password, key)
                .runCatching {
                    start()
                }.onFailure {
                    _result.postValue(it)
                    it.printStackTrace()
                }.onSuccess {
                    if (it) {
                        _result.postValue(null)
                    }
                }
        }
    }
}
