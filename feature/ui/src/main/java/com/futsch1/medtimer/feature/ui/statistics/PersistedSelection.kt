package com.futsch1.medtimer.feature.ui.statistics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A user selection seeded from persistence and written through on change. The active view and the
 * Analysis range are the two adapters at this seam, so the "skip if unchanged, otherwise persist and
 * update" rule lives here once instead of being restated in each selection handler.
 */
class PersistedSelection<T>(
    initial: T,
    private val persist: (T) -> Unit,
) {
    private val _value = MutableStateFlow(initial)
    val value: StateFlow<T> = _value.asStateFlow()

    fun set(next: T) {
        if (next == _value.value) return
        persist(next)
        _value.value = next
    }
}
