package moe.shizuku.manager.stealth.ui

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.appendRandomSuffix
import moe.shizuku.manager.core.utils.ApkUtils
import java.io.File

sealed class UiState {
    data class Idle(
        val action: Action,
    ) : UiState()

    object Loading : UiState()

    data class Pending(
        val apk: File,
        val apkType: ApkType,
    ) : UiState()

    data class Error(
        val error: Exception,
    ) : UiState()
}

enum class Action {
    HIDE,
    UNHIDE,
    REHIDE,
}

enum class ApkType {
    CLONE,
    STUB,
}

class StealthViewModel(
    private val context: Context,
    private val apkUtils: ApkUtils
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Idle(Action.HIDE))
    val uiState: LiveData<UiState> = _uiState

    private var _packageName: String? = null

    private fun isShizukuHidden() =
        runCatching {
            context.packageManager.getPackageInfo(ApkUtils.ORIGINAL_PACKAGE_NAME, 0)
        }.isFailure

    init {
        refresh()
    }

    fun refresh() {
        val action =
            if (isShizukuHidden()) {
                Action.UNHIDE
            } else if (context.packageName == ApkUtils.ORIGINAL_PACKAGE_NAME) {
                Action.HIDE
            } else {
                Action.REHIDE
            }
        _uiState.value = UiState.Idle(action)
    }

    fun setPackageName(packageName: String? = null) {
        _packageName = packageName ?: context.packageName.appendRandomSuffix()
    }

    fun createApk(apkType: ApkType) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.postValue(UiState.Loading)

                val apk =
                    when (apkType) {
                        ApkType.CLONE -> {
                            apkUtils.changePackageName(
                                File(context.applicationInfo.sourceDir),
                                _packageName!!,
                                maybeCreateSigningKey = true
                            )
                        }

                        ApkType.STUB -> {
                            apkUtils.createStubApk(ApkUtils.ORIGINAL_PACKAGE_NAME)
                        }
                    }

                _uiState.postValue(UiState.Pending(apk, apkType))
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error(e))
                Log.e("StealthTutorialViewModel", "Error creating APK", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // WorkDir is now managed by ApkUtils if needed, or we can just clean up cache.
        // For now, let's assume we don't need to manually delete workDir if it's in cache.
    }
}

@StringRes
fun String.validatePackageName(): Int? =
    when {
        isBlank() -> null

        !matches(Regex("^[a-zA-Z0-9.]+$")) -> {
            R.string.package_name_error_invalid_characters
        }

        split('.').any { it.firstOrNull()?.isDigit() == true } -> {
            R.string.package_name_error_segment_starts_with_number
        }

        split('.').size < 2 -> {
            R.string.package_name_error_needs_two_segments
        }

        endsWith('.') -> {
            R.string.package_name_error_ends_with_period
        }

        else -> null
    }
