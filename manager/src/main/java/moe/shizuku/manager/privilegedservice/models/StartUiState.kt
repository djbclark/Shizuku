package moe.shizuku.manager.privilegedservice.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import moe.shizuku.manager.core.utils.runnable.RunnableStatus

data class StartUiState(
    val steps: List<StartStepUiModel> = emptyList(),
    val status: RunnableStatus = RunnableStatus.Pending
)

data class StartStepUiModel(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val status: RunnableStatus
)