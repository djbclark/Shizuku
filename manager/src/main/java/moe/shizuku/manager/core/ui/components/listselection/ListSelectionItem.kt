package moe.shizuku.manager.core.ui.components.listselection

data class ListSelectionItem<out T>(
    val value: T,
    val label: CharSequence,
    val description: CharSequence? = null,
    val isEnabled: Boolean = true,
    val iconRes: Int? = null,
    val type: Type = Type.NONE
) {
    enum class Type {
        RADIO, ICON, NONE
    }
}
