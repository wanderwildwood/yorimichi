package com.wanderwildwood.yorimichi.walk

import com.wanderwildwood.yorimichi.core.Attractor
import com.wanderwildwood.yorimichi.core.Coord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedWalkTest {

    private val walk = Attractor(
        point = Coord(51.4779, -0.0015),
        distanceMetres = 812.5,
        bearingDegrees = 243.0,
        density = 2.75,
        concentration = 1.4,
        points = 1024,
        radiusMetres = 2000.0,
    )

    @Test fun aWalkComesBackAsItWent() {
        assertEquals(walk, attractorFromSaved(walk.toSaved()))
    }

    @Test fun nothingSavedIsNoWalk() {
        assertNull(attractorFromSaved(null))
    }

    @Test fun aWrongShapeIsNoWalk() {
        assertNull(attractorFromSaved(doubleArrayOf(1.0, 2.0)))
        assertNull(attractorFromSaved(DoubleArray(8) { Double.NaN }))
    }
}
