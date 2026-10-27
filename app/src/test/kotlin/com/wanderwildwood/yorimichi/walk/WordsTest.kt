package com.wanderwildwood.yorimichi.walk

import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.device.Fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordsTest {

    @Test
    fun `short walks are said in feet and long ones in miles`() {
        assertEquals(Distance.Feet(300), distance(91.44, Units.IMPERIAL))
        val long = distance(1_000.0, Units.IMPERIAL) as Distance.Miles
        assertEquals("0.6", oneDecimal(long.miles))
    }

    @Test
    fun `short walks are said in metres and long ones in kilometres`() {
        assertEquals(Distance.Metres(90), distance(91.44, Units.METRIC))
        val long = distance(1_000.0, Units.METRIC) as Distance.Kilometres
        assertEquals("1.0", oneDecimal(long.kilometres))
    }

    @Test
    fun `the compass has sixteen points and wraps`() {
        assertEquals(CompassPoint.N, cardinal(0.0))
        assertEquals(CompassPoint.NNE, cardinal(22.5))
        assertEquals(CompassPoint.E, cardinal(90.0))
        assertEquals(CompassPoint.NW, cardinal(315.0))
        assertEquals(CompassPoint.N, cardinal(359.0))
        assertEquals(CompassPoint.N, cardinal(360.0))
    }

    @Test
    fun `a bearing reads as a name and a number`() {
        assertEquals(Bearing(CompassPoint.NNE, 32), bearing(32.0))
    }

    @Test
    fun `a coordinate is written for another app to read`() {
        assertEquals("35.68120, 139.76710", coordinate(Coord(35.6812, 139.7671)))
    }

    @Test
    fun `an ordinary cluster is called ordinary`() {
        assertEquals(Reading.Ordinary, reading(1.0))
    }

    @Test
    fun `a real cluster gets its number said`() {
        val thick = reading(3.0) as Reading.Thicker
        assertEquals("3.0", oneDecimal(thick.times))
    }

    @Test
    fun `a thin patch is described as thin`() {
        val thin = reading(0.2) as Reading.Thinner
        assertEquals("5.0", oneDecimal(thin.times))
    }

    @Test
    fun `a very good fix never reads as zero`() {
        assertEquals(FixLine.GoodTo(Distance.Metres(10)), fixLine(Fix(Coord(0.0, 0.0), 0.4f, NOW), Units.METRIC, NOW))
        assertEquals(FixLine.GoodTo(Distance.Feet(30)), fixLine(Fix(Coord(0.0, 0.0), 1f, NOW), Units.IMPERIAL, NOW))
        assertEquals(FixLine.GoodTo(Distance.Metres(20)), fixLine(Fix(Coord(0.0, 0.0), 19f, NOW), Units.METRIC, NOW))
    }

    @Test
    fun `units follow the phone's country`() {
        assertEquals(Units.IMPERIAL, unitsForCountry("US"))
        assertEquals(Units.IMPERIAL, unitsForCountry("gb"))
        assertEquals(Units.METRIC, unitsForCountry("CA"))
        assertEquals(Units.METRIC, unitsForCountry(""))
        assertEquals(Units.METRIC, unitsForCountry(null))
    }

    @Test
    fun `the fix line says what is known about the fix`() {
        assertEquals(
            FixLine.GoodTo(Distance.Metres(10)),
            fixLine(Fix(Coord(0.0, 0.0), 8f, NOW), Units.METRIC, NOW),
        )
    }

    @Test
    fun `an old fix says how old`() {
        assertEquals(
            FixLine.Stale(Age.Minutes(20)),
            fixLine(Fix(Coord(0.0, 0.0), 5f, NOW - 20 * 60_000), Units.METRIC, NOW),
        )
        assertEquals(
            FixLine.Stale(Age.Hours(2)),
            fixLine(Fix(Coord(0.0, 0.0), 5f, NOW - 2 * 3_600_000), Units.METRIC, NOW, locationOn = false),
        )
    }

    @Test
    fun `a fix grows old as the clock moves on`() {
        val fix = Fix(Coord(0.0, 0.0), 5f, NOW)
        assertEquals(FixLine.GoodTo(Distance.Metres(10)), fixLine(fix, Units.METRIC, NOW + 60_000))
        assertEquals(FixLine.Stale(Age.Minutes(3)), fixLine(fix, Units.METRIC, NOW + 3 * 60_000))
    }

    @Test
    fun `with no position at all the line says why`() {
        assertEquals(FixLine.Searching, fixLine(null, Units.METRIC, NOW, searchedMillis = 30_000))
        assertEquals(FixLine.NotFound, fixLine(null, Units.METRIC, NOW, searchedMillis = GIVE_UP_MILLIS))
        assertEquals(
            FixLine.Off,
            fixLine(null, Units.METRIC, NOW, locationOn = false, searchedMillis = GIVE_UP_MILLIS),
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

/** An invented moment, for the ages above to be measured from. */
private const val NOW = 1_800_000_000_000L
