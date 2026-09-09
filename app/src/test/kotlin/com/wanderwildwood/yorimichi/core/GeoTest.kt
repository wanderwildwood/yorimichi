package com.wanderwildwood.yorimichi.core

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {

    private val here = Coord(35.6812, 139.7671)

    @Test
    fun `a kilometre north measures a kilometre`() {
        assertEquals(1000.0, here.distanceTo(here.offset(east = 0.0, north = 1000.0)), 1.0)
    }

    @Test
    fun `a kilometre east measures a kilometre`() {
        assertEquals(1000.0, here.distanceTo(here.offset(east = 1000.0, north = 0.0)), 1.0)
    }

    @Test
    fun `a kilometre east still measures a kilometre well north`() {
        // The longitude scaling is the part that goes wrong away from the equator.
        val far = Coord(64.1466, -21.9426)
        assertEquals(1000.0, far.distanceTo(far.offset(east = 1000.0, north = 0.0)), 1.0)
    }

    @Test
    fun `bearings point where they should`() {
        assertEquals(0.0, here.bearingTo(here.offset(0.0, 1000.0)), 0.1)
        assertEquals(90.0, here.bearingTo(here.offset(1000.0, 0.0)), 0.1)
        assertEquals(180.0, here.bearingTo(here.offset(0.0, -1000.0)), 0.1)
        assertEquals(270.0, here.bearingTo(here.offset(-1000.0, 0.0)), 0.1)
    }

    @Test
    fun `bearings are never negative`() {
        // A bearing is fed straight to an arrow, so it has to come out on 0..360 and
        // not as the -90 that atan2 would hand back for west.
        val west = here.bearingTo(here.offset(-1000.0, 0.0))
        assert(west in 0.0..360.0) { "bearing was $west" }
    }

    @Test
    fun `distance is the same in both directions`() {
        val there = here.offset(2500.0, -1800.0)
        assertEquals(here.distanceTo(there), there.distanceTo(here), 0.001)
    }
}
