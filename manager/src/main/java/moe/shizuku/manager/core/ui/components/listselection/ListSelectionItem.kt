package moe.shizuku.manager.core.ui.components.listselection

interface ListSelectionItem {
    val label: CharSequence? get() = null
    val labelRes: Int get() = 0
    val description: CharSequence? get() = null
    val descriptionRes: Int? get() = null
    val isEnabled: Boolean get() = true
    val iconRes: Int? get() = null
    val type: Type get() = Type.NONE

    enum class Type {
        RADIO, LINK, NONE
    }
}