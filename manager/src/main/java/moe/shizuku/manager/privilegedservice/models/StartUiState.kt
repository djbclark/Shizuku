package moe.shizuku.manager.privilegedservice.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import moe.shizuku.manager.core.utils.runnable.RunnableStatus

data class StartUiState(
    val steps: List<StartStepItem> = emptyList(),
    val status: RunnableStatus = RunnableStatus.Pending
)

data class StartStepItem(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val status: RunnableStatus
)