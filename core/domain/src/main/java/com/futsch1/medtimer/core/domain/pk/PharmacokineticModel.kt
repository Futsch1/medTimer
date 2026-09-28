package com.futsch1.medtimer.core.domain.pk

import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.ln

data class Dose(val time: Instant, val amount: Double)

/**
 * One-compartment model with first-order absorption and elimination (Bateman function).
 *
 * The result is the amount of drug in the body in dose units, assuming full bioavailability. It is only meant to show the
 * shape of the level over time, not an absolute concentration.
 *
 * @param halfLifeHours elimination half-life, must be positive
 * @param timeToPeakHours time from a single dose to its peak level; 0 means instant absorption (e.g. IV bolus)
 */
class PharmacokineticModel(val halfLifeHours: Double, timeToPeakHours: Double) {
    init {
        require(halfLifeHours > 0.0) { "Half-life must be positive" }
        require(!timeToPeakHours.isNaN()) { "Time to peak must be a number" }
    }

    /** Elimination rate constant in 1/h. */
    val eliminationRate: Double = LN2 / halfLifeHours

    /** Absorption rate constant in 1/h, or null for instant absorption. */
    val absorptionRate: Double? = absorptionRateFor(eliminationRate, timeToPeakHours)

    /** Time to peak in hours that this model actually produces (may be clamped at the extremes, see [absorptionRateFor]). */
    val effectiveTimeToPeakHours: Double
        get() = absorptionRate?.let { ka ->
            if (ka == eliminationRate) 1.0 / ka else ln(ka / eliminationRate) / (ka - eliminationRate)
        } ?: 0.0

    /**
     * After this many hours a dose has practically no effect anymore (less than 0.1 % of its amount remains). The level falls with
     * the slower of absorption and elimination, which is absorption when it is slower than elimination ("flip-flop" kinetics).
     */
    val relevanceHours: Double
        get() = (RELEVANT_HALF_LIVES * LN2 / minOf(absorptionRate ?: eliminationRate, eliminationRate) + effectiveTimeToPeakHours)
            .coerceAtMost(MAX_RELEVANCE_HOURS)

    /** Amount remaining from a single dose of [amount] taken [hours] ago. */
    fun singleDoseAmount(amount: Double, hours: Double): Double {
        if (hours < 0.0) {
            return 0.0
        }
        val ke = eliminationRate
        val ka = absorptionRate ?: return amount * exp(-ke * hours)
        if (ka == ke) {
            return amount * ke * hours * exp(-ke * hours)
        }
        // ka/(ka-ke) * (e^(-ke t) - e^(-ka t)), factored around the slower rate so that it neither overflows for old doses when
        // absorption is slower than elimination, nor loses accuracy (expm1) when ka is close to ke
        val slow = minOf(ka, ke)
        val fast = maxOf(ka, ke)
        return amount * ka / (fast - slow) * exp(-slow * hours) * -expm1(-(fast - slow) * hours)
    }

    fun amountAt(doses: List<Dose>, time: Instant): Double =
        doses.sumOf { dose -> singleDoseAmount(dose.amount, hoursBetween(dose.time, time)) }

    /**
     * Samples the level between [from] and [to]. On top of an even grid of [gridPoints] points, the level is sampled just before
     * every dose and at every dose's expected peak, so the troughs and peaks are captured regardless of the grid resolution.
     * Doses older than [relevanceHours] at a sample time are ignored, which keeps sampling long histories cheap.
     */
    fun sample(doses: List<Dose>, from: Instant, to: Instant, gridPoints: Int = DEFAULT_GRID_POINTS): List<Pair<Instant, Double>> {
        require(gridPoints >= 2)
        if (!to.isAfter(from)) {
            return emptyList()
        }
        val step = Duration.between(from, to).toMillis() / (gridPoints - 1)
        val times = sortedSetOf<Instant>()
        for (i in 0 until gridPoints - 1) {
            times.add(from.plusMillis(step * i))
        }
        times.add(to)
        val peakOffsetMillis = (effectiveTimeToPeakHours * MILLIS_PER_HOUR).toLong()
        for (dose in doses) {
            listOf(dose.time.minusSeconds(1), dose.time, dose.time.plusMillis(peakOffsetMillis))
                .filter { !it.isBefore(from) && !it.isAfter(to) }
                .forEach { times.add(it) }
        }
        val sortedDoses = doses.sortedBy { it.time }
        val relevanceMillis = (relevanceHours * MILLIS_PER_HOUR).toLong()
        var first = 0
        var end = 0
        // Times are ascending, so the window of relevant doses only ever moves forward
        return times.map { time ->
            val earliest = time.minusMillis(relevanceMillis)
            while (first < sortedDoses.size && sortedDoses[first].time.isBefore(earliest)) first++
            while (end < sortedDoses.size && !sortedDoses[end].time.isAfter(time)) end++
            time to (first until end).sumOf { i -> singleDoseAmount(sortedDoses[i].amount, hoursBetween(sortedDoses[i].time, time)) }
        }
    }

    companion object {
        const val DEFAULT_GRID_POINTS = 300
        private val LN2 = ln(2.0)
        private const val MILLIS_PER_HOUR = 3_600_000.0

        /** Beyond this ratio of ka/ke absorption is practically instant. */
        private const val MAX_RATE_RATIO = 1e9

        /** Below this ratio of ka/ke absorption is too slow to matter; the ratio is clamped here. */
        private const val MIN_RATE_RATIO = 1e-9

        /** Within this distance of a ka/ke ratio of 1, the ka == ke limit is used. */
        private const val EQUAL_RATE_TOLERANCE = 1e-6
        private const val BISECTION_STEPS = 100
        private const val RELEVANT_HALF_LIVES = 10

        /** Caps [relevanceHours] for absurd parameters (about 114 years), keeping the date arithmetic in range. */
        private const val MAX_RELEVANCE_HOURS = 1e6

        private fun hoursBetween(from: Instant, to: Instant): Double = Duration.between(from, to).toMillis() / MILLIS_PER_HOUR

        /**
         * Solves tmax = ln(ka/ke) / (ka - ke) for ka (Shargel & Ducharme, Applied Biopharmaceutics and Pharmacokinetics, 8th ed.,
         * Eq. 16.17). With x = ka/ke this is ln(x)/(x-1) = ke * tmax, where the left side falls monotonically from infinity
         * (x -> 0) through 1 (x = 1, where ka == ke and the peak is at 1/ke) to 0 (x -> infinity), so there is exactly one solution:
         * absorption faster than elimination for a time to peak below 1/ke, and slower ("flip-flop") above it.
         */
        fun absorptionRateFor(eliminationRate: Double, timeToPeakHours: Double): Double? {
            if (timeToPeakHours <= 0.0) {
                return null
            }
            val target = eliminationRate * timeToPeakHours
            if (target <= peakFactor(MAX_RATE_RATIO)) {
                return null
            }
            if (target >= peakFactor(MIN_RATE_RATIO)) {
                return eliminationRate * MIN_RATE_RATIO
            }
            if (target in peakFactor(1.0 + EQUAL_RATE_TOLERANCE)..peakFactor(1.0 - EQUAL_RATE_TOLERANCE)) {
                return eliminationRate
            }
            var low = ln(if (target < 1.0) 1.0 + EQUAL_RATE_TOLERANCE else MIN_RATE_RATIO)
            var high = ln(if (target < 1.0) MAX_RATE_RATIO else 1.0 - EQUAL_RATE_TOLERANCE)
            repeat(BISECTION_STEPS) {
                val mid = (low + high) / 2
                if (peakFactor(exp(mid)) > target) low = mid else high = mid
            }
            return eliminationRate * exp((low + high) / 2)
        }

        private fun peakFactor(ratio: Double): Double = ln(ratio) / (ratio - 1.0)
    }
}
