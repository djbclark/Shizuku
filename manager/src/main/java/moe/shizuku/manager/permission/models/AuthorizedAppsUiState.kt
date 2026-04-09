package moe.shizuku.manager.permission.models

sealed class AuthorizedAppsUiState {
    object Loading : AuthorizedAppsUiState()
    data class Result(
        val apps: List<AuthorizedAppsItem.App>,
        val isRefreshing: Boolean = false
    ) : AuthorizedAppsUiState() {
        val isAppListEmpty: Boolean get() = apps.isEmpty()
        val areAllAppsGranted: Boolean get() = !isAppListEmpty && apps.all { it.isGranted }
    }
}
