package moe.shizuku.manager.stealth.ui

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsMargin
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.dp
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.StealthFragmentBinding
import moe.shizuku.manager.core.utils.ApkUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class StealthFragment : Fragment(R.layout.stealth_fragment) {
    private val viewModel: StealthViewModel by viewModel()
    private val apkUtils: ApkUtils by inject()
    private val binding by viewBinding(StealthFragmentBinding::bind)
    private lateinit var outDir: Uri

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        with(binding) {
            viewModel.uiState.observe(viewLifecycleOwner) { state ->
                val isLoadingOrPending = (state is UiState.Loading || state is UiState.Pending)
                fab.isInvisible = isLoadingOrPending
                loadingFab.isVisible = isLoadingOrPending

                when (state) {
                    is UiState.Idle -> {
                        val action = state.action

                        packageNameContainer.isVisible = (action == Action.HIDE)
                        if (packageNameContainer.isVisible) makeNavBarTransparent()

                        fab.update(action)
                        fab.setOnClickListener { onClick(action) }
                    }

                    is UiState.Loading -> {}

                    is UiState.Pending -> {
                        try {
                            val apk = state.apk
                            when (state.apkType) {
                                ApkType.CLONE -> {
                                    export(apk)
                                    viewModel.refresh()
                                    showUninstallDialog()
                                }

                                ApkType.STUB -> {
                                    apkUtils.installPackage(apk) { isSuccess, msg ->
                                        handleInstallerResult(
                                            isSuccess,
                                            msg
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            showErrorDialog(e)
                        }
                    }

                    is UiState.Error -> {
                        showErrorDialog(state.error)
                    }
                }
            }

            packageNameEditText.apply {
                addTextChangedListener { text ->
                    val input = text.toString()

                    packageNameLayout.error =
                        input.validatePackageName()?.let { getString(it) }

                    packageNameLayout.helperText =
                        if (input.isEmpty()) {
                            getString(R.string.stealth_package_name_helper_text)
                        } else {
                            null
                        }

                    fab.isEnabled = (packageNameLayout.error == null)
                }
            }

            scrollView.applySystemBarsPadding(start = true, end = true)
            packageNameContainer.applySystemBarsPadding(bottom = true)
            packageNameLayout.applySystemBarsPadding(start = true, end = true)
            fab.applySystemBarsMargin(start = true, end = true)
            loadingFab.applySystemBarsMargin(start = true, end = true)

            ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                val fabBottomMargin =
                    if (packageNameContainer.isVisible) (-12).dp else (systemBarsInsets.bottom + 16.dp)

                listOf(fab, loadingFab).forEach {
                    it.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = fabBottomMargin
                    }
                }
                insets
            }

            root.viewTreeObserver.addOnGlobalLayoutListener {
                scrollView.updatePadding(bottom = root.height - fab.top)
            }
        }
    }

    private fun onClick(action: Action) {
        when (action) {
            Action.HIDE -> {
                val packageName = binding.packageNameEditText.text.toString().ifEmpty { null }
                viewModel.setPackageName(packageName)

                showChooseFolderDialog()
            }

            Action.UNHIDE -> {
                viewModel.createApk(ApkType.STUB)
            }

            Action.REHIDE -> {
                apkUtils.uninstallPackage(ApkUtils.ORIGINAL_PACKAGE_NAME) { isSuccess, msg ->
                    handleInstallerResult(
                        isSuccess,
                        msg
                    )
                }
            }
        }
    }

    private fun showChooseFolderDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.stealth_choose_folder)
            .setMessage(R.string.stealth_choose_folder_message)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.stealth_choose_folder) { _, _ ->
                pickFolderLauncher.launch(null)
            }.show()
    }

    private val pickFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
            if (folder == null) return@registerForActivityResult
            outDir = folder

            viewModel.createApk(ApkType.CLONE)
        }

    private fun export(apk: File) {
        val docUri =
            DocumentsContract.buildDocumentUriUsingTree(
                outDir,
                DocumentsContract.getTreeDocumentId(outDir),
            )

        val doc =
            DocumentsContract.createDocument(
                requireContext().contentResolver,
                docUri,
                "application/vnd.android.package-archive",
                apkUtils.buildApkFilename(),
            )

        if (doc == null) throw Exception("Could not create file in selected folder")

        requireContext().contentResolver.openOutputStream(doc)?.use { output ->
            apk.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    private fun showUninstallDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.stealth_uninstall_required)
            .setMessage(R.string.stealth_uninstall_message)
            .setPositiveButton(R.string.uninstall) { _, _ ->
                apkUtils.uninstallPackage(requireContext().packageName)
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun handleInstallerResult(isSuccess: Boolean, msg: String?) {
        viewModel.refresh()
        if (isSuccess) {
            toast(R.string.success)
        } else {
            showErrorDialog(Exception(msg ?: "Unknown error"))
        }
    }

    private fun showErrorDialog(error: Exception) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.error)
            .setMessage(error.message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun ExtendedFloatingActionButton.update(action: Action) {
        if (action == Action.UNHIDE) {
            setIconResource(R.drawable.ic_visibility_on_filled_24)
            text = getString(R.string.unhide)
        } else {
            setIconResource(R.drawable.ic_visibility_off_filled_24)
            text = getString(R.string.hide)
        }
    }

    private val backCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val uiState = viewModel.uiState.value
                if (uiState is UiState.Idle || uiState is UiState.Error) {
                    findNavController().popBackStack()
                    return
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("${getString(android.R.string.cancel)}?")
                    .setPositiveButton("Yes") { _, _ -> findNavController().popBackStack() }
                    .setNegativeButton("No", null)
                    .show()
            }
        }

    private fun makeNavBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireActivity().window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireActivity().window.isNavigationBarContrastEnforced = false
        }
    }


}
