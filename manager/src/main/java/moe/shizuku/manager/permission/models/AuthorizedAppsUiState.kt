package moe.shizuku.manager.permission.models

data class AuthorizedAppsUiState(
    val apps: List<AuthorizedAppsItem.App> = emptyList()
) {
    val isAppListEmpty: Boolean get() = apps.isEmpty()
    val areAllAppsGranted: Boolean get() = !isAppListEmpty && apps.all { it.isGranted }
}
