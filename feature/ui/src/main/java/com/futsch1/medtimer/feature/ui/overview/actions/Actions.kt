package com.futsch1.medtimer.feature.ui.overview.actions


interface Actions {
    suspend fun buttonClicked(button: Button)

    val visibleButtons: List<Button>
}
