package com.futsch1.medtimer.core.domain.pk

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

/**
 * Checks the model against worked examples from Shargel & Ducharme, Applied Biopharmaceutics and Pharmacokinetics, 8th edition
 * (McGraw Hill, ISBN 978-1-260-14299-0). The model works in amounts, but in a one-compartment model concentration is simply
 * amount / V, so the book's concentrations are compared as amount / V.
 */
class PharmacokineticModelTextbookTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")

    private fun hours(h: Double): Instant = start.plusMillis((h * 3_600_000).toLong())

    /** Eq. 16.17: tmax = ln(ka/kel) / (ka - kel), whose limit for ka == kel is 1/kel. */
    private fun timeToPeak(ka: Double, kel: Double) = if (ka == kel) 1.0 / kel else ln(ka / kel) / (ka - kel)

    private fun modelFor(ka: Double, kel: Double) = PharmacokineticModel(halfLifeHours = ln(2.0) / kel, timeToPeakHours = timeToPeak(ka, kel))

    /** The book's values are rounded to 3 significant figures. */
    private fun assertRounded(expected: Double, actual: Double, message: String) =
        assertEquals(message, expected, actual, max(0.01, abs(expected) * 0.01))

    private data class Table16Row(val ka: Double, val kel: Double, val tmax: Double, val cmax: Double, val auc: Double)

    /** Table 16-2: single oral dose of 100 mg, F = 1, V = 10 L. Includes flip-flop rows where ka < kel. */
    private val table16 = listOf(
        Table16Row(0.1, 0.2, 6.93, 2.50, 50.0),
        Table16Row(0.2, 0.1, 6.93, 5.00, 100.0),
        Table16Row(0.3, 0.1, 5.49, 5.77, 100.0),
        Table16Row(0.4, 0.1, 4.62, 6.29, 100.0),
        Table16Row(0.5, 0.1, 4.02, 6.69, 100.0),
        Table16Row(0.6, 0.1, 3.58, 6.99, 100.0),
        Table16Row(0.3, 0.2, 4.05, 4.44, 50.0),
        Table16Row(0.3, 0.3, 3.33, 3.68, 33.3),
        Table16Row(0.3, 0.4, 2.88, 3.16, 25.0),
        Table16Row(0.3, 0.5, 2.55, 2.79, 20.0),
    )

    @Test
    fun `table 16-2 the time to peak determines the absorption rate`() {
        for (row in table16) {
            val model = PharmacokineticModel(halfLifeHours = ln(2.0) / row.kel, timeToPeakHours = row.tmax)

            // tmax is printed with 2 decimals, which limits how exactly ka can be recovered
            assertEquals("ka for $row", row.ka, model.absorptionRate!!, row.ka * 0.005)
        }
    }

    @Test
    fun `table 16-2 peak concentrations`() {
        for (row in table16) {
            val model = PharmacokineticModel(halfLifeHours = ln(2.0) / row.kel, timeToPeakHours = row.tmax)
            val cmax = model.singleDoseAmount(100.0, model.effectiveTimeToPeakHours) / 10.0

            assertEquals("Cmax for $row", row.cmax, cmax, 0.015)
        }
    }

    @Test
    fun `table 16-2 exposure only depends on elimination`() {
        for (row in table16) {
            val model = modelFor(row.ka, row.kel)
            // Trapezoidal AUC until the level has practically vanished; the printed values are D / (V kel), i.e. AUC to infinity
            val step = 0.01
            val auc = (0 until (model.relevanceHours * 3 / step).toInt()).sumOf { i ->
                (model.singleDoseAmount(100.0, i * step) + model.singleDoseAmount(100.0, (i + 1) * step)) / 2 * step
            } / 10.0

            assertEquals("AUC for $row", row.auc, auc, 0.1)
        }
    }

    /** Table 17-1, "Dose 1" column: µg/mL after a single oral dose, t1/2 = 4 h, ka = 1.5 /h, V = 10 L, F = 1. */
    private val table17SingleDose = listOf(
        21.0, 22.3, 19.8, 16.9, 14.3, 12.0, 10.1, 8.50, 7.15, 6.01, 5.06, 4.25,
        3.58, 3.01, 2.53, 2.13, 1.79, 1.51, 1.27, 1.07, 0.90, 0.75, 0.63, 0.53,
    )

    /** Table 17-1, "Total" column: the same dose every 4 hours, 6 doses, from 1 h to 24 h. */
    private val table17Total = listOf(
        21.0, 22.3, 19.8, 16.9, 35.3, 34.3, 29.9, 25.4, 42.5, 40.3, 35.0, 29.7,
        46.0, 43.3, 37.5, 31.8, 47.8, 44.8, 38.8, 32.9, 48.7, 45.6, 39.4, 33.4,
    )

    // The footnote states a 350 mg dose, but the printed concentrations are exactly those of 300 mg; only the shape matters here
    private val table17Dose = 300.0

    @Test
    fun `table 17-1 single dose curve`() {
        val model = modelFor(ka = 1.5, kel = ln(2.0) / 4.0)

        table17SingleDose.forEachIndexed { index, expected ->
            val t = index + 1.0
            assertRounded(expected, model.singleDoseAmount(table17Dose, t) / 10.0, "single dose at $t h")
        }
    }

    @Test
    fun `table 17-1 superposition of repeated doses`() {
        val model = modelFor(ka = 1.5, kel = ln(2.0) / 4.0)
        val doses = (0 until 6).map { Dose(hours(it * 4.0), table17Dose) }

        table17Total.forEachIndexed { index, expected ->
            val t = index + 1.0
            assertRounded(expected, model.amountAt(doses, hours(t)) / 10.0, "total at $t h")
        }
    }

    @Test
    fun `chapter 12 question 1 IV bolus data`() {
        // 300 mg IV bolus; fitted kel = 0.17028 /h and back-extrapolated C0 = 8.57 mg/L
        val model = PharmacokineticModel(halfLifeHours = ln(2.0) / 0.17028, timeToPeakHours = 0.0)
        val measured = listOf(0.25 to 8.21, 0.5 to 7.87, 1.0 to 7.23, 3.0 to 5.15, 6.0 to 3.09, 12.0 to 1.11, 18.0 to 0.40)

        measured.forEach { (t, expected) -> assertRounded(expected, model.singleDoseAmount(8.57, t), "Cp at $t h") }
        // 1b: about 10 half-lives, 40.7 h, until 99.9 % is eliminated
        assertEquals(0.001, model.singleDoseAmount(1.0, 40.7), 0.0001)
    }

    @Test
    fun `chapter 12 questions 2 and 4 fraction remaining after 24 hours`() {
        // Q2: t1/2 = 6 h, 200 mg: 12.5 mg left, 93.75 % lost
        assertEquals(12.5, PharmacokineticModel(6.0, 0.0).singleDoseAmount(200.0, 24.0), 1e-9)
        // Q4: t1/2 = 8 h, 600 mg: 75 mg left, 87.5 % lost
        assertEquals(75.0, PharmacokineticModel(8.0, 0.0).singleDoseAmount(600.0, 24.0), 1e-9)
    }

    @Test
    fun `chapter 12 question 3 time to reach the MIC`() {
        // kel = 0.228 /h, C0 = 8.48 mg/L: the MIC of 0.5 mg/L is reached after 12.4 h
        val model = PharmacokineticModel(halfLifeHours = ln(2.0) / 0.228, timeToPeakHours = 0.0)

        assertEquals(0.5, model.singleDoseAmount(8.48, 12.4), 0.005)
        assertEquals(8.48 * exp(-0.228 * 12.4), model.singleDoseAmount(8.48, 12.4), 1e-9)
    }
}
