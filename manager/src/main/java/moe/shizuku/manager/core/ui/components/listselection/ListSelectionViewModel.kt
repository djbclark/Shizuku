package moe.shizuku.manager.core.ui.components.listselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ListSelectionViewModel : ViewModel() {
    var items: List<ListSelectionItem<Any>> = emptyList()
    var selectedItem: Any? = null

    private val _results = MutableSharedFlow<Any>(extraBufferCapacity = 1)
    val results = _results.asSharedFlow()

    fun select(value: Any) {
        viewModelScope.launch {
            _results.emit(value)
        }
    }
}
