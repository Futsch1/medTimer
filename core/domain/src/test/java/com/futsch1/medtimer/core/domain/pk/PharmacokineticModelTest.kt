package com.futsch1.medtimer.core.domain.pk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.ln

class PharmacokineticModelTest {

    private val start = Instant.parse("2026-01-01T00:00:00Z")

    private fun hours(h: Double): Instant = start.plusMillis((h * 3_600_000).toLong())

    @Test
    fun `instant absorption halves after one half-life`() {
        val model = PharmacokineticModel(halfLifeHours = 4.8, timeToPeakHours = 0.0)

        assertNull(model.absorptionRate)
        assertEquals(10.0, model.singleDoseAmount(10.0, 0.0), 1e-9)
        assertEquals(5.0, model.singleDoseAmount(10.0, 4.8), 1e-9)
        assertEquals(2.5, model.singleDoseAmount(10.0, 9.6), 1e-9)
    }

    @Test
    fun `solved absorption rate reproduces requested time to peak`() {
        for ((halfLife, tmax) in listOf(120.0 to 24.0, 120.0 to 72.0, 4.8 to 1.0, 4.8 to 2.0, 10.0 to 0.1)) {
            val model = PharmacokineticModel(halfLife, tmax)
            assertEquals(tmax, model.effectiveTimeToPeakHours, 1e-6)
        }
    }

    @Test
    fun `single dose peaks at time to peak`() {
        val model = PharmacokineticModel(halfLifeHours = 120.0, timeToPeakHours = 48.0)
        val peak = model.singleDoseAmount(1.0, 48.0)

        assertTrue(model.singleDoseAmount(1.0, 47.0) < peak)
        assertTrue(model.singleDoseAmount(1.0, 49.0) < peak)
        assertEquals(0.0, model.singleDoseAmount(1.0, 0.0), 1e-12)
        assertEquals(0.0, model.singleDoseAmount(1.0, -1.0), 1e-12)
    }

    @Test
    fun `a time to peak beyond 1 over ke means absorption is slower than elimination`() {
        // 1/ke = 120 / ln 2 = 173 h, so a Tmax of 200 h needs ka < ke ("flip-flop")
        val model = PharmacokineticModel(halfLifeHours = 120.0, timeToPeakHours = 200.0)

        assertTrue(model.absorptionRate!! < model.eliminationRate)
        assertEquals(200.0, model.effectiveTimeToPeakHours, 1e-6)
        // The level then falls with the slower absorption, so old doses stay relevant for longer
        assertEquals(10 * ln(2.0) / model.absorptionRate!! + 200.0, model.relevanceHours, 1e-6)
    }

    @Test
    fun `slow absorption stays finite for very old doses`() {
        val model = PharmacokineticModel(halfLifeHours = 4.8, timeToPeakHours = 20.0)

        for (h in listOf(0.001, 1.0, 100.0, 1e4, 1e6)) {
            val amount = model.singleDoseAmount(1.0, h)
            assertTrue("amount at $h h: $amount", amount.isFinite() && amount >= 0.0)
        }
    }

    @Test
    fun `a time to peak of exactly 1 over ke uses equal rates`() {
        val model = PharmacokineticModel(halfLifeHours = 10.0, timeToPeakHours = 10.0 / ln(2.0))

        assertEquals(model.eliminationRate, model.absorptionRate!!, 0.0)
    }

    @Test
    fun `absurd parameters keep the relevance window in range`() {
        val model = PharmacokineticModel(halfLifeHours = 1e6, timeToPeakHours = 1e9)

        assertTrue(model.relevanceHours <= 1e6)
        assertTrue(model.sample(listOf(Dose(hours(0.0), 1.0)), hours(0.0), hours(10.0)).isNotEmpty())
    }

    @Test
    fun `time to peak close to the limit stays finite`() {
        val model = PharmacokineticModel(halfLifeHours = 120.0, timeToPeakHours = 173.0)

        for (h in listOf(0.001, 1.0, 100.0, 1000.0)) {
            val amount = model.singleDoseAmount(1.0, h)
            assertTrue(amount.isFinite() && amount >= 0.0)
        }
    }

    @Test
    fun `doses superpose linearly`() {
        val model = PharmacokineticModel(halfLifeHours = 4.8, timeToPeakHours = 1.5)
        val a = Dose(hours(0.0), 1.5)
        val b = Dose(hours(3.0), 3.0)
        val t = hours(5.0)

        assertEquals(model.amountAt(listOf(a), t) + model.amountAt(listOf(b), t), model.amountAt(listOf(a, b), t), 1e-12)
        assertEquals(0.0, model.amountAt(listOf(b), hours(2.0)), 0.0)
    }

    @Test
    fun `weekly dosing accumulates towards steady state`() {
        val model = PharmacokineticModel(halfLifeHours = 120.0, timeToPeakHours = 0.0)
        val interval = 168.0
        val doses = (0 until 20).map { Dose(hours(it * interval), 1.0) }

        // Right after the last dose, the amount approaches the geometric accumulation limit
        val expected = 1.0 / (1.0 - exp(-model.eliminationRate * interval))
        assertEquals(expected, model.amountAt(doses, hours(19 * interval)), 1e-3)
    }

    @Test
    fun `sampling covers troughs and peaks`() {
        val model = PharmacokineticModel(halfLifeHours = 4.8, timeToPeakHours = 1.5)
        val doses = listOf(Dose(hours(10.0), 1.0), Dose(hours(20.0), 1.0))

        val samples = model.sample(doses, hours(0.0), hours(240.0), gridPoints = 2)
        val times = samples.map { it.first }

        assertEquals(hours(0.0), times.first())
        assertEquals(hours(240.0), times.last())
        assertTrue(times.contains(hours(11.5)))
        assertTrue(times.contains(hours(21.5)))
        assertTrue(times.contains(hours(10.0).minus(Duration.ofSeconds(1))))
        assertEquals(times.sorted(), times)
        // The second peak sits on top of what is left of the first dose
        assertEquals(model.amountAt(doses, hours(21.5)), samples.maxOf { it.second }, 1e-12)
    }

    @Test
    fun `sampling ignores doses that have practically decayed`() {
        val model = PharmacokineticModel(halfLifeHours = 1.0, timeToPeakHours = 0.0)
        val old = Dose(hours(0.0), 1.0)
        val recent = Dose(hours(100.0), 1.0)

        val sample = model.sample(listOf(recent, old), hours(100.0), hours(101.0), gridPoints = 2)

        // Unsorted input is fine, and the 100 h old dose is skipped instead of adding its 2^-100 remainder
        assertEquals(1.0, sample.first().second, 0.0)
        assertEquals(0.5, sample.last().second, 1e-12)
        assertTrue(model.relevanceHours < 100.0)
    }

    @Test
    fun `sampling an empty range returns nothing`() {
        val model = PharmacokineticModel(halfLifeHours = 4.8, timeToPeakHours = 1.5)

        assertTrue(model.sample(emptyList(), hours(1.0), hours(1.0)).isEmpty())
    }
}
