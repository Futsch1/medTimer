package com.futsch1.medtimer.feature.ui.overview.actions


interface Actions {
    suspend fun buttonClicked(visitor: ActionsVisitor)

    val visibleButtons: List<Button>
}
