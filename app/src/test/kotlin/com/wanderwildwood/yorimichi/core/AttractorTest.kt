package com.wanderwildwood.yorimichi.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt
import kotlin.random.Random

class AttractorTest {

    private val here = Coord(35.6812, 139.7671)

    private fun run(
        selector: Selector = Selector.Densest,
        radius: Double = 2000.0,
        points: Int = 512,
        seed: Int = 7,
    ) = generate(here, radius, points, selector, Random(seed))

    @Test
    fun `the scatter fills the disc evenly rather than crowding the middle`() {
        // Half the radius encloses a quarter of the area, so about a quarter of the
        // points. Without the square root in the sampling this comes out near a half,
        // which is the failure that would never announce itself.
        val (east, north) = scatter(1000.0, 20_000, Random(11))
        val inner = east.indices.count { sqrt(east[it] * east[it] + north[it] * north[it]) < 500.0 }
        assertEquals(0.25, inner / 20_000.0, 0.02)
    }

    @Test
    fun `no point falls outside the radius`() {
        val (east, north) = scatter(1500.0, 5_000, Random(3))
        val furthest = east.indices.maxOf { sqrt(east[it] * east[it] + north[it] * north[it]) }
        assertTrue("furthest was $furthest", furthest <= 1500.0)
    }

    @Test
    fun `the destination is inside the radius asked for`() {
        val attractor = run(radius = 3000.0)
        assertTrue(
            "landed ${attractor.distanceMetres} m out",
            attractor.distanceMetres <= 3000.0,
        )
    }

    @Test
    fun `the same seed gives the same place twice`() {
        assertEquals(run().point, run().point)
    }

    @Test
    fun `a different seed gives a different place`() {
        assertNotEquals(run(seed = 1).point, run(seed = 2).point)
    }

    @Test
    fun `the densest cell is denser than the emptiest`() {
        assertTrue(run(Selector.Densest).density > run(Selector.Emptiest).density)
    }

    @Test
    fun `the median cell sits between the two`() {
        val median = run(Selector.Quantile(0.5)).density
        assertTrue(median > run(Selector.Emptiest).density)
        assertTrue(median < run(Selector.Densest).density)
    }

    @Test
    fun `an attractor is thicker than chance and a void is thinner`() {
        assertTrue(run(Selector.Densest).concentration > 1.0)
        assertTrue(run(Selector.Emptiest).concentration < 1.0)
    }

    @Test
    fun `concentration is around one for an unremarkable cell`() {
        // The middle of the sorted densities is by construction an ordinary spot, and
        // the honest number for an ordinary spot is one. Loose bounds: this is a
        // statistic over 512 points, not a constant.
        val ordinary = run(Selector.Quantile(0.5)).concentration
        assertTrue("was $ordinary", ordinary > 0.4 && ordinary < 2.5)
    }

    @Test
    fun `bearing and distance agree with the point itself`() {
        val attractor = run()
        assertEquals(here.distanceTo(attractor.point), attractor.distanceMetres, 0.001)
        assertEquals(here.bearingTo(attractor.point), attractor.bearingDegrees, 0.001)
    }

    @Test
    fun `a small walk still works`() {
        // 200 m and few points is the degenerate end: the bandwidth floor and the
        // concentration floor both have to hold it up rather than divide by zero.
        val attractor = generate(here, 200.0, 16, Selector.Densest, Random(5))
        assertTrue(attractor.distanceMetres <= 200.0)
        assertTrue(attractor.concentration.isFinite())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a radius of nothing is refused`() {
        generate(here, 0.0, 512, Selector.Densest, Random(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `one point is refused`() {
        generate(here, 1000.0, 1, Selector.Densest, Random(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a quantile outside nought to one is refused`() {
        generate(here, 1000.0, 512, Selector.Quantile(1.5), Random(1))
    }
}
