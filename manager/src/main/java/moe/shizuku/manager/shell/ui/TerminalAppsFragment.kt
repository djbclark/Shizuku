package moe.shizuku.manager.shell.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.showSnackbar
import moe.shizuku.manager.core.platform.device.RomInfo
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.TerminalAppsFragmentBinding
import java.io.IOException

private const val SH_NAME = "rish"
private const val DEX_NAME = "rish_shizuku.dex"
private const val COPY_CMD = "cp /sdcard/path/to/chosen-folder/$SH_NAME* ~"
private const val SHELL_CMD = "sh $SH_NAME"

class TerminalAppsFragment : Fragment(R.layout.terminal_apps_fragment) {
    private val binding by viewBinding(TerminalAppsFragmentBinding::bind)

    private val openFolderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { dirUri: Uri? ->
            dirUri?.let { exportFiles(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            root.applySystemBarsPadding(bottom = true, start = true, end = true)

            miui.isVisible = RomInfo.isMiui

            button1.setOnClickListener { openFolderPicker.launch(null) }
            command2.text = COPY_CMD
            command3.text = SHELL_CMD
        }
    }

    private fun exportFiles(dirUri: Uri) =
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val context = requireContext().applicationContext

            runCatching {
                val dir = DocumentFile.fromTreeUri(context, dirUri)
                    ?: throw IOException("Failed to open selected folder")

                listOf(SH_NAME, DEX_NAME).forEach { fileName ->
                    dir.findFile(fileName)?.delete()

                    val mimeType = when (fileName) {
                        SH_NAME -> "text/x-shellscript"
                        DEX_NAME -> "application/octet-stream"
                        else -> throw IOException("Unknown file: $fileName")
                    }
                    val file = dir.createFile(mimeType, fileName)
                        ?: throw IOException("Failed to create file: $fileName")

                    val fileStream = context.contentResolver.openOutputStream(file.uri)
                        ?: throw IOException("Failed to open output stream for file: $fileName")

                    fileStream.use { stream ->
                        context.assets.open(fileName).use { asset ->
                            if (fileName == SH_NAME) {
                                val script = asset.bufferedReader().readText()
                                    .replace("MANAGER_PKG", context.packageName)
                                stream.write(script.toByteArray())
                            } else {
                                asset.copyTo(stream)
                            }
                        }
                    }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    showSnackbar(R.string.export_success)
                }
            }.onFailure {
                if (it is CancellationException) throw it

                Log.e(TAG, "Export failed", it)
                withContext(Dispatchers.Main) {
                    showSnackbar(R.string.export_failed)
                }
            }
        }
}
