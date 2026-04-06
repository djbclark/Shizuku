package moe.shizuku.manager.core.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

suspend fun <T> Flow<T>.collectAsEvents(onEvent: (T) -> Unit) =
    withContext(Dispatchers.Main.immediate) {
        collect(onEvent)
    }