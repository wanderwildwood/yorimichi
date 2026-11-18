package com.wanderwildwood.yorimichi.walk

import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.core.offset
import com.wanderwildwood.yorimichi.device.Fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    private companion object {
        /** A clock well past every fix below, unless a test says otherwise. */
        const val NOW = 10_000_000L
    }

    private val point = Coord(10.0, 20.0)
    private fun fix(c: Coord, t: Long = 1_000_000L, acc: Float = 8f, gps: Boolean = false) = Fix(c, acc, t, gps)

    @Test
    fun `the distance shrinks as the walker closes in and the bearing stays on the point`() {
        val start = point.offset(east = 0.0, north = -500.0)
        val a = leg(start, point)
        val b = leg(point.offset(0.0, -200.0), point)
        val c = leg(point.offset(0.0, -60.0), point)
        assertEquals(500.0, a.distanceMetres, 2.0)
        assertEquals(200.0, b.distanceMetres, 2.0)
        assertEquals(60.0, c.distanceMetres, 1.0)
        assertEquals(0.0, c.bearingDegrees, 0.0)
    }

    @Test
    fun `the bearing turns when the walker is off to one side`() {
        assertEquals(270.0, leg(point.offset(300.0, 0.0), point).bearingDegrees, 1.0)
        assertEquals(90.0, leg(point.offset(-300.0, 0.0), point).bearingDegrees, 1.0)
    }

    @Test
    fun `arrival is within twenty five metres and not before`() {
        assertFalse(leg(point.offset(0.0, -40.0), point).arrived)
        assertFalse(leg(point.offset(0.0, -26.0), point).arrived)
        assertTrue(leg(point.offset(0.0, -24.0), point).arrived)
        assertTrue(leg(point, point).arrived)
    }

    @Test
    fun `a position goes stale after a minute and no position is stale`() {
        val f = fix(point, t = 0)
        assertFalse(staleWhileWalking(f, 59_000))
        assertTrue(staleWhileWalking(f, 61_000))
        assertTrue(staleWhileWalking(null, 0))
    }

    @Test
    fun `a newer fix replaces an older one and an older one never does`() {
        val old = fix(point, t = 10_000)
        assertTrue(accept(null, old, now = NOW))
        assertTrue(accept(old, fix(point, t = 11_000), now = NOW))
        assertFalse(accept(old, fix(point, t = 9_000), now = NOW))
    }

    @Test
    fun `a clock set back does not freeze the position on the fix held from before`() {
        // Held at 100 s by the old clock; the clock has since gone back to 50 s.
        val held = fix(point, t = 100_000, acc = 6f, gps = true)
        assertTrue(accept(held, fix(point, t = 49_000, acc = 6f, gps = true), now = 50_000))
        assertTrue(accept(held, fix(point, t = 49_000, acc = 900f), now = 50_000))
        // With the clock where it was, an older fix is still refused.
        assertFalse(accept(held, fix(point, t = 99_000, gps = true), now = 101_000))
    }

    @Test
    fun `a coarse network fix does not displace a good gps fix unless the gps has gone quiet`() {
        val gps = fix(point, t = 100_000, acc = 6f, gps = true)
        assertFalse(accept(gps, fix(point, t = 103_000, acc = 900f), now = NOW))
        assertTrue(accept(gps, fix(point, t = 100_000 + QUIET_GPS_MILLIS, acc = 900f), now = NOW))
        assertTrue(accept(gps, fix(point, t = 103_000, acc = 5f), now = NOW))
    }

    @Test
    fun `a newer gps fix is taken even when it is a little less sure`() {
        val held = fix(point, t = 100_000, acc = 6f, gps = true)
        assertTrue(accept(held, fix(point, t = 101_000, acc = 9f, gps = true), now = NOW))
        // A precise fix remembered from earlier does not hold off the live GPS either.
        assertTrue(accept(fix(point, t = 100_000, acc = 3f), fix(point, t = 101_000, acc = 12f, gps = true), now = NOW))
    }

    @Test
    fun `the state follows the fix and has no leg without a walk or a position`() {
        val attractor = com.wanderwildwood.yorimichi.core.Attractor(point, 500.0, 0.0, 0.0, 1.0, 1, 2000.0)
        val far = WalkState(fix = fix(point.offset(0.0, -500.0)), attractor = attractor, now = 1_000_000L)
        assertEquals(500.0, far.leg!!.distanceMetres, 2.0)
        val near = far.copy(fix = fix(point.offset(0.0, -10.0)))
        assertTrue(near.leg!!.arrived)
        assertNull(far.copy(attractor = null).leg)
        assertNull(far.copy(fix = null).leg)
    }

    @Test
    fun `arrived only on a live position sure enough to tell`() {
        val attractor = com.wanderwildwood.yorimichi.core.Attractor(point, 500.0, 0.0, 0.0, 1.0, 1, 2000.0)
        val here = WalkState(fix = fix(point.offset(0.0, -10.0), t = 0), attractor = attractor, now = 10_000)
        assertTrue(here.leg!!.arrived)
        // A coarse network fix that happens to land on the point.
        val coarse = here.copy(fix = fix(point.offset(0.0, -10.0), t = 0, acc = 400f))
        assertFalse(coarse.leg!!.arrived)
        assertEquals(10.0, coarse.leg!!.distanceMetres, 1.0)
        // The position is lost: old, or location switched off.
        assertFalse(here.copy(now = 90_000).leg!!.arrived)
        assertFalse(here.copy(locationOn = false).leg!!.arrived)
    }

    @Test
    fun `position lost when old or when location is off`() {
        val s = WalkState(fix = fix(point, t = 0), now = 30_000)
        assertFalse(s.positionLost)
        assertTrue(s.copy(now = 90_000).positionLost)
        assertTrue(s.copy(locationOn = false).positionLost)
        assertTrue(s.copy(fix = null).positionLost)
    }
}
