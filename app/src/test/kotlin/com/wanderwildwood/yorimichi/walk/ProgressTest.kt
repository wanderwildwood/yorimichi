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
    private val point = Coord(10.0, 20.0)
    private fun fix(c: Coord, t: Long = 1_000_000L, acc: Float = 8f) = Fix(c, acc, t)

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
        assertTrue(accept(null, old))
        assertTrue(accept(old, fix(point, t = 11_000)))
        assertFalse(accept(old, fix(point, t = 9_000)))
    }

    @Test
    fun `a coarse network fix does not displace a good gps fix unless the gps has gone quiet`() {
        val gps = fix(point, t = 100_000, acc = 6f)
        assertFalse(accept(gps, fix(point, t = 103_000, acc = 900f)))
        assertTrue(accept(gps, fix(point, t = 100_000 + QUIET_GPS_MILLIS, acc = 900f)))
        assertTrue(accept(gps, fix(point, t = 103_000, acc = 5f)))
    }

    @Test
    fun `the state follows the fix and has no leg without a walk or a position`() {
        val attractor = com.wanderwildwood.yorimichi.core.Attractor(point, 500.0, 0.0, 0.0, 1.0, 1, 2000.0)
        val far = WalkState(fix = fix(point.offset(0.0, -500.0)), attractor = attractor)
        assertEquals(500.0, far.leg!!.distanceMetres, 2.0)
        val near = far.copy(fix = fix(point.offset(0.0, -10.0)))
        assertTrue(near.leg!!.arrived)
        assertNull(far.copy(attractor = null).leg)
        assertNull(far.copy(fix = null).leg)
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
