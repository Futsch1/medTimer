package com.futsch1.medtimer.feature.ui

/**
 * The parts of the shared options menu that only the app module can perform — backups, the intro
 * screen, test-data generation and the version string all live in `:app`.
 *
 * Bound to its `:app` implementation via Hilt. This replaces the former `OptionsMenuFactory`, which
 * had to hand `Fragment`, `NavController` and `Menu` across the module boundary to build a
 * `MenuProvider`; the menu itself is now a composable in this module.
 */
interface AppOptionsActions {
    val versionName: String
    val isDebugBuild: Boolean

    fun createBackup()
    fun restoreBackup()
    fun configureAutomaticBackup()
    fun generateTestData(withEvents: Boolean)
    fun showAppIntro()
    fun onDestroy()
}

/** Bound to the `:app` implementation via Hilt; created per hosting fragment in `onCreate`. */
fun interface AppOptionsActionsFactory {
    fun create(fragment: androidx.fragment.app.Fragment): AppOptionsActions
}
