package com.futsch1.medtimer.feature.ui.statistics.levels

import com.futsch1.medtimer.core.common.helpers.MedicineHelper
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.model.ReminderEvent
import com.futsch1.medtimer.core.domain.pk.Dose
import com.futsch1.medtimer.core.domain.pk.DoseAmountParser
import com.futsch1.medtimer.core.domain.pk.DoseUnitSummary
import com.futsch1.medtimer.core.domain.pk.DoseUnits
import com.futsch1.medtimer.core.domain.pk.PharmacokineticModel
import com.futsch1.medtimer.core.ui.TimeFormatter
import kotlinx.collections.immutable.toImmutableList
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Estimates the level of every medicine with half-life and time to peak set from its taken doses over the last [days] days (including
 * today, matching the Charts' bar chart). Doses are matched to medicines by normalized name, like the other analysis views, since
 * manual doses carry no reminder. The unit of the doses does not matter as each curve is normalized, but a medicine whose doses use
 * different units is not estimated at all and reported in [LevelsState.unitConflicts] instead. Doses whose amount cannot be read are
 * left out and counted in [LevelsState.ignoredDoses].
 */
class LevelsPresenter @Inject constructor(
    private val timeFormatter: TimeFormatter,
) {
    fun present(
        events: List<ReminderEvent>,
        medicines: List<Medicine>,
        days: Int,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): LevelsState {
        val today = now.atZone(zone).toLocalDate()
        val takenByName = events
            .filter { it.status == ReminderEvent.ReminderStatus.TAKEN }
            .groupBy { MedicineHelper.normalizeMedicineName(it.medicineName) }
            .mapValues { (_, taken) -> taken.sortedBy { it.doseTime() } }
        val classifiedByName = takenByName.mapValues { (_, taken) -> classifyDoses(taken) }
        val dosesByName = classifiedByName.mapValues { (_, classified) -> classified.labelled.map { it.first } }

        val (modelled, conflicting) = medicines
            .mapNotNull { medicine -> medicine.pharmacokineticModel()?.let { medicine to it } }
            .distinctBy { it.first.name }
            // Each curve is normalized, so any unit works, but only if all doses of a medicine share it
            .partition { (medicine, _) -> classifiedByName[medicine.name]?.units?.consistent ?: true }
        val unitConflicts = conflicting.map { (medicine, _) ->
            UnitConflict(medicine.name, classifiedByName.getValue(medicine.name).units.units.toImmutableList())
        }
        // A dose taken just now has not raised the level yet, so project far enough ahead to show its peak
        val projectionDays = modelled.maxOfOrNull { ceil(it.second.effectiveTimeToPeakHours / LevelsState.HOURS_PER_DAY).toInt() } ?: 0

        // The chart can be scrolled back through the whole dose history (up to a limit); [days] only sets the visible span
        val earliestDoseDay = modelled
            .flatMap { dosesByName[it.first.name].orEmpty() }
            .minOfOrNull { it.time }
            ?.atZone(zone)?.toLocalDate()
        val firstDay = listOfNotNull(earliestDoseDay, today.minusDays(days - 1L)).min()
            .coerceAtLeast(today.minusDays(MAX_HISTORY_DAYS - 1L))
        val historyDays = ChronoUnit.DAYS.between(firstDay, today).toInt() + 1
        val windowStart = firstDay.atStartOfDay()
        val windowEnd = windowStart.plusDays(historyDays.toLong() + projectionDays)
        val visibleHours = (days + projectionDays) * LevelsState.HOURS_PER_DAY
        val ignoredDoses = modelled.sumOf { (medicine, _) ->
            classifiedByName[medicine.name]?.unreadable.orEmpty().count { event ->
                event.doseTime().atZone(zone).toLocalDateTime().let { !it.isBefore(windowStart) && it.isBefore(windowEnd) }
            }
        }

        val series = modelled.map { (medicine, model) ->
            val start = windowStart.atZone(zone).toInstant()
            val end = windowEnd.atZone(zone).toInstant()
            val doses = relevantDoses(model, dosesByName[medicine.name].orEmpty(), start, end)
            val past = model.sample(doses, start, now, gridPoints(start, now, visibleHours))
            val projection = model.sample(doses, now, end, gridPoints(now, end, visibleHours))
            // Normalized over the whole history, so the curve keeps its scale while scrolling
            val max = (past + projection).maxOfOrNull { it.second } ?: 0.0
            val doseMarkers = classifiedByName[medicine.name]?.labelled.orEmpty()
                .filter { (dose, _) -> !dose.time.isBefore(start) && !dose.time.isAfter(end) }
                .map { (dose, label) ->
                    val level = model.amountAt(doses, dose.time)
                    DoseMarker(localHoursBetween(windowStart, dose.time, zone), if (max > 0.0) level / max * 100.0 else 0.0, label)
                }
            LevelSeries(
                medicineName = medicine.name,
                color = medicine.color.takeIf { medicine.useColor },
                past = toCurve(past, max, windowStart, zone),
                projection = toCurve(projection, max, windowStart, zone),
                doses = doseMarkers.toImmutableList(),
            )
        }

        return LevelsState(
            series = series.toImmutableList(),
            days = days,
            historyDays = historyDays,
            projectionDays = projectionDays,
            ignoredDoses = ignoredDoses,
            unitConflicts = unitConflicts.toImmutableList(),
            nowHours = localHoursBetween(windowStart, now, zone),
            midnightLabels = (0..historyDays + projectionDays).map { dayLabel(firstDay.plusDays(it.toLong())) }.toImmutableList(),
        )
    }

    private fun relevantDoses(model: PharmacokineticModel, doses: List<Dose>, from: Instant, to: Instant): List<Dose> {
        // Doses long before the window have practically decayed; skip them to keep the sampling cheap
        val earliest = from.minusSeconds((model.relevanceHours * SECONDS_PER_HOUR).toLong())
        return doses.filter { !it.time.isBefore(earliest) && !it.time.isAfter(to) }
    }

    /** Keeps the grid resolution of the visible span constant, however long the scrollable history is. */
    private fun gridPoints(from: Instant, to: Instant, visibleHours: Double): Int {
        val hours = Duration.between(from, to).toMillis() / (SECONDS_PER_HOUR * 1000.0)
        return ceil(PharmacokineticModel.DEFAULT_GRID_POINTS * hours / visibleHours).toInt().coerceIn(2, MAX_GRID_POINTS)
    }

    private fun toCurve(samples: List<Pair<Instant, Double>>, max: Double, windowStart: LocalDateTime, zone: ZoneId) = LevelCurve(
        hours = samples.map { localHoursBetween(windowStart, it.first, zone) }.toImmutableList(),
        percent = samples.map { if (max > 0.0) it.second / max * 100.0 else 0.0 }.toImmutableList(),
    )

    private fun dayLabel(date: LocalDate): String = timeFormatter.daysSinceEpochToDateString(date.toEpochDay())

    /** @param labelled readable doses with their amount as recorded, for labeling */
    private class ClassifiedDoses(val units: DoseUnitSummary, val labelled: List<Pair<Dose, String>>, val unreadable: List<ReminderEvent>)

    private fun classifyDoses(taken: List<ReminderEvent>): ClassifiedDoses {
        val labelled = mutableListOf<Pair<Dose, String>>()
        val unreadable = mutableListOf<ReminderEvent>()
        for (event in taken) {
            val dose = DoseAmountParser.parse(event.amount)
            if (dose != null) labelled.add(Dose(event.doseTime(), dose.amount) to event.amount.trim()) else unreadable.add(event)
        }
        return ClassifiedDoses(DoseUnits.summarize(taken.map { it.amount }), labelled, unreadable)
    }

    private fun ReminderEvent.doseTime(): Instant = if (processedTimestamp != Instant.EPOCH) processedTimestamp else remindedTimestamp

    companion object {
        private const val MAX_HISTORY_DAYS = 365
        private const val MAX_GRID_POINTS = 5000
        private const val SECONDS_PER_HOUR = 3600.0

        /** Wall-clock hours, so that local midnights stay on multiples of 24 across daylight saving changes. */
        fun localHoursBetween(start: LocalDateTime, instant: Instant, zone: ZoneId): Double =
            Duration.between(start, instant.atZone(zone).toLocalDateTime()).toMillis() / (SECONDS_PER_HOUR * 1000.0)
    }
}
