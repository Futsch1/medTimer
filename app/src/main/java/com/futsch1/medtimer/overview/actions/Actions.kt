package com.futsch1.medtimer.overview.actions


interface Actions {
    suspend fun buttonClicked(button: Button)

    val visibleButtons: List<Button>
}
