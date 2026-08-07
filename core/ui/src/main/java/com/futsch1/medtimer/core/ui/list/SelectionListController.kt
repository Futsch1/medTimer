package com.futsch1.medtimer.core.ui.list

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Selection state for a list screen: the items, whether selection mode is active, and which ids are
 * selected. Held in snapshot state so a view-model can own it and Compose can read it directly.
 *
 * @param idOf identity of an item, used for selection bookkeeping.
 */
class SelectionListController<T : Any>(
    private val idOf: (T) -> Any,
) {
    var items: ImmutableList<T> by mutableStateOf(persistentListOf())
        private set

    var isInSelectionMode: Boolean by mutableStateOf(false)
        private set

    var selectedIds: Set<Any> by mutableStateOf(emptySet())
        private set

    val selectedItems: ImmutableList<T> by derivedStateOf {
        items.filter { idOf(it) in selectedIds }.toImmutableList()
    }

    /** Subscribes the items source. Call from the view-model's init. */
    fun bind(scope: CoroutineScope, itemsFlow: Flow<List<T>>) {
        itemsFlow
            .onEach { newItems ->
                items = newItems.toPersistentList()
                // Items can disappear underneath the selection (deleted, filtered out).
                selectedIds = selectedIds intersect newItems.map(idOf).toSet()
                if (selectedIds.isEmpty()) {
                    isInSelectionMode = false
                }
            }
            .launchIn(scope)
    }

    fun isSelected(item: T): Boolean = idOf(item) in selectedIds

    fun enterSelectionMode() {
        isInSelectionMode = true
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedIds = emptySet()
    }

    fun toggleSelection(item: T) {
        val itemId = idOf(item)
        selectedIds = if (itemId in selectedIds) selectedIds - itemId else selectedIds + itemId
        if (selectedIds.isEmpty()) {
            isInSelectionMode = false
        }
    }

    fun selectAll() {
        selectedIds = items.map(idOf).toSet()
    }
}
