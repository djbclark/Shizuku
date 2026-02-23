package moe.shizuku.manager.shell.ui

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.TerminalAppsFragmentBinding
import rikka.compatibility.DeviceCompatibility
import rikka.insets.initialPaddingBottom
import rikka.insets.initialPaddingLeft
import rikka.insets.initialPaddingRight
import rikka.insets.initialPaddingTop
import rikka.insets.setInitialPadding
import kotlin.math.roundToInt

private const val SH_NAME = "rish"
private const val DEX_NAME = "rish_shizuku.dex"

class TerminalAppsFragment : Fragment() {
    private val openDocumentsTree =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree: Uri? ->
            if (tree == null) return@registerForActivityResult

            val cr = requireContext().contentResolver
            val doc = DocumentsContract.buildDocumentUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree)
            )
            val child =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    tree,
                    DocumentsContract.getTreeDocumentId(tree)
                )

            cr
                .query(
                    child,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    ),
                    null,
                    null,
                    null,
                )?.use {
                    while (it.moveToNext()) {
                        val id = it.getString(0)
                        val name = it.getString(1)
                        if (name == SH_NAME || name == DEX_NAME) {
                            DocumentsContract.deleteDocument(
                                cr,
                                DocumentsContract.buildDocumentUriUsingTree(tree, id)
                            )
                        }
                    }
                }

            fun writeToDocument(name: String) {
                DocumentsContract.createDocument(
                    requireContext().contentResolver,
                    doc,
                    "application/octet-stream",
                    name
                )?.runCatching {
                    cr.openOutputStream(this)?.let { output ->
                        requireContext().assets.open(name).use { input ->
                            if (name == SH_NAME) {
                                input.bufferedReader().use {
                                    val text =
                                        it
                                            .readText()
                                            .replace("MANAGER_PKG", requireContext().packageName)
                                    output.write(text.toByteArray())
                                }
                            } else {
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            writeToDocument(SH_NAME)
            writeToDocument(DEX_NAME)
        }

    private var _binding: TerminalAppsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TerminalAppsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.content.apply {
            setInitialPadding(
                initialPaddingLeft,
                initialPaddingTop + (resources.displayMetrics.density * 8).roundToInt(),
                initialPaddingRight,
                initialPaddingBottom,
            )
        }

        binding.apply {
            if (DeviceCompatibility.isMiui()) {
                miui.isVisible = true
            }

            text1.text = getString(R.string.terminal_tutorial_1, SH_NAME, DEX_NAME)

            text2.text = getString(R.string.terminal_tutorial_2)
            command2.text = "cp /sdcard/chosen-folder/* /data/data/terminal.package.name/files"
            summary2.text = getString(R.string.terminal_tutorial_2_tip, SH_NAME, SH_NAME, ".bashrc")

            text3.text = getString(R.string.terminal_tutorial_3)
            command3.text = "sh /path/to/$SH_NAME"

            button1.setOnClickListener { openDocumentsTree.launch(null) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}