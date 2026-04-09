package moe.shizuku.manager.core.ui.components.listselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ListSelectionViewModel : ViewModel() {
    var items: List<ListSelectionItem> = emptyList()
    var selectedItem: Any? = null

    private val _results = Channel<Any>(capacity = Channel.BUFFERED)
    val results: Flow<Any> = _results.receiveAsFlow()

    fun select(value: Any) {
        viewModelScope.launch {
            _results.send(value)
        }
    }
}
