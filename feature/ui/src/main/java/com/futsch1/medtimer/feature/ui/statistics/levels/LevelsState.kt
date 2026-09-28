package com.futsch1.medtimer.feature.ui.statistics.levels

import androidx.compose.runtime.Immutable
import com.futsch1.medtimer.core.domain.pk.UnitCount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Render-ready estimated medicine levels. The x-axis is in local wall-clock hours since the start of the first day in the window,
 * so every multiple of 24 is a local midnight. The window covers the last [historyDays] days (today included), followed by
 * [projectionDays] days in which the level is projected assuming no further doses. Only the last [days] days and the projection
 * are visible at once; the chart scrolls back through the rest.
 *
 * @param ignoredDoses taken doses in the window that are left out because their amount cannot be read
 * @param unitConflicts medicines that are not estimated because their doses use different units
 * @param nowHours position of the current time on the x-axis, where the projection starts
 * @param midnightLabels date label of each midnight from the window start up to and including the window end
 */
@Immutable
data class LevelsState(
    val series: ImmutableList<LevelSeries>,
    val days: Int,
    val historyDays: Int,
    val projectionDays: Int,
    val ignoredDoses: Int,
    val unitConflicts: ImmutableList<UnitConflict>,
    val nowHours: Double,
    val midnightLabels: ImmutableList<String>,
) {
    val windowHours: Double get() = (historyDays + projectionDays) * HOURS_PER_DAY
    val visibleHours: Double get() = (days + projectionDays) * HOURS_PER_DAY

    companion object {
        const val HOURS_PER_DAY = 24.0
    }
}

/**
 * One medicine's level curve, normalized so that its highest value in the window (projection included) is 100.
 *
 * @param color the medicine's custom color (ARGB), or null to use a theme color that contrasts in light and dark mode
 * @param past the level from the window start up to now
 * @param projection the level from now to the window end, assuming no further doses
 * @param doses the doses taken within the window, to label on the curve
 */
@Immutable
data class LevelSeries(
    val medicineName: String,
    val color: Int?,
    val past: LevelCurve,
    val projection: LevelCurve,
    val doses: ImmutableList<DoseMarker> = persistentListOf(),
)

/** A dose at [hours] on the x-axis, where the curve is at [percent], labeled with the amount as recorded. */
@Immutable
data class DoseMarker(
    val hours: Double,
    val percent: Double,
    val label: String,
)

/** A medicine left out of the estimate because its recorded doses use several [units] ("" for doses without a unit). */
@Immutable
data class UnitConflict(
    val medicineName: String,
    val units: ImmutableList<UnitCount>,
)

@Immutable
data class LevelCurve(
    val hours: ImmutableList<Double>,
    val percent: ImmutableList<Double>,
)
