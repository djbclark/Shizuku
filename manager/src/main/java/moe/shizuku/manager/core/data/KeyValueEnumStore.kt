package moe.shizuku.manager.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object KeyValueEnumStore {
    private val dataSource = KeyValueDataSource

    internal inline fun <reified E> get(entry: KeyValueEntry<E>): E
            where E : Enum<E>, E : KeyValueEnum =
        dataSource.get(entry.asIntEntry()).asEnum() ?: entry.default

    internal inline fun <reified E> observe(entry: KeyValueEntry<E>): Flow<E>
            where E : Enum<E>, E : KeyValueEnum =
        dataSource.observe(entry.asIntEntry())
            .map { intValue ->
                intValue.asEnum() ?: entry.default
            }

    internal fun <E> set(entry: KeyValueEntry<E>, value: E)
            where E : Enum<E>, E : KeyValueEnum =
        dataSource.set(entry.asIntEntry(), value.value)

    // Helper functions

    private fun <E> KeyValueEntry<E>.asIntEntry(): KeyValueEntry<Int>
            where E : Enum<E>, E : KeyValueEnum =
        KeyValueEntry(key, default.value)

    private inline fun <reified E> Int.asEnum(): E?
            where E : Enum<E>, E : KeyValueEnum =
        enumValues<E>().firstOrNull { it.value == this }
}
