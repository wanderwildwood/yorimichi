package com.wanderwildwood.yorimichi.walk

import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.device.Fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordsTest {

    @Test
    fun `short walks are said in feet and long ones in miles`() {
        assertEquals("300 ft", distance(91.44, Units.IMPERIAL))
        assertEquals("0.6 mi", distance(1_000.0, Units.IMPERIAL))
    }

    @Test
    fun `short walks are said in metres and long ones in kilometres`() {
        assertEquals("90 m", distance(91.44, Units.METRIC))
        assertEquals("1.0 km", distance(1_000.0, Units.METRIC))
    }

    @Test
    fun `the compass has sixteen points and wraps`() {
        assertEquals("N", cardinal(0.0))
        assertEquals("NNE", cardinal(22.5))
        assertEquals("E", cardinal(90.0))
        assertEquals("NW", cardinal(315.0))
        assertEquals("N", cardinal(359.0))
        assertEquals("N", cardinal(360.0))
    }

    @Test
    fun `a bearing reads as a name and a number`() {
        assertEquals("NNE 32°", bearing(32.0))
    }

    @Test
    fun `a coordinate is written for another app to read`() {
        assertEquals("35.68120, 139.76710", coordinate(Coord(35.6812, 139.7671)))
    }

    @Test
    fun `an ordinary cluster is called ordinary`() {
        assertEquals("No thicker here than chance usually gives.", reading(1.0))
    }

    @Test
    fun `a real cluster gets its number said`() {
        assertEquals(
            "The points fell 3.0 times thicker here than an even scatter.",
            reading(3.0),
        )
    }

    @Test
    fun `a thin patch is described as thin`() {
        assertEquals(
            "The points fell 5.0 times thinner here than an even scatter.",
            reading(0.2),
        )
    }

    @Test
    fun `the fix line says what is known about the fix`() {
        assertEquals("Waiting for a fix", fixLine(null, Units.METRIC))
        assertEquals(
            "Fix good to 10 m",
            fixLine(Fix(Coord(0.0, 0.0), 8f, 0), Units.METRIC),
        )
    }

    @Test
    fun `an old fix says how old`() {
        assertEquals(
            "Last fix 20 min ago",
            fixLine(Fix(Coord(0.0, 0.0), 5f, 20 * 60_000), Units.METRIC),
        )
    }

    @Test
    fun `changing units moves the walk to the nearest one offered`() {
        // A mile is not 1,609 metres to anybody choosing a walk; it is "1 mile" in one
        // list and "2 km" in the other.
        assertEquals(2_000, nearestRadius(1_609, Units.METRIC).metres)
        assertEquals(1_609, nearestRadius(2_000, Units.IMPERIAL).metres)
    }

    @Test
    fun `whatever walk is stored, it snaps to one that has a name`() {
        // A stored walk that is not on the current list has no label to show, and the
        // screen fell back to saying "2000 m" under units that speak in miles.
        for (units in Units.entries) {
            for (stored in listOf(0, 400, 500, 1_609, 2_000, 100_000)) {
                assertTrue(radii(units).contains(nearestRadius(stored, units)))
            }
        }
    }
}
