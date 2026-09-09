package com.wanderwildwood.yorimichi.core

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A point on the earth, in degrees. */
data class Coord(val lat: Double, val lon: Double)

/**
 * Metres in one degree of latitude, taking the earth as a sphere of radius 6371 km.
 * Wrong by a few parts in a thousand, which is well inside the error of the phone's
 * own fix and far inside the error of walking to a point in a field.
 */
const val METRES_PER_DEGREE: Double = 6_371_000.0 * 2.0 * PI / 360.0

/** The point [east] metres east and [north] metres north of this one. */
fun Coord.offset(east: Double, north: Double): Coord {
    val latitude = lat + north / METRES_PER_DEGREE
    val longitude = lon + east / (METRES_PER_DEGREE * cos(lat * PI / 180.0))
    return Coord(latitude, longitude)
}

/** Great-circle distance in metres. */
fun Coord.distanceTo(other: Coord): Double {
    val phi1 = lat * PI / 180.0
    val phi2 = other.lat * PI / 180.0
    val dPhi = phi2 - phi1
    val dLambda = (other.lon - lon) * PI / 180.0

    val a = sin(dPhi / 2) * sin(dPhi / 2) +
        cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
    return 2 * 6_371_000.0 * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * Initial great-circle bearing to [other], in degrees clockwise from true north.
 *
 * Initial rather than constant: over the distances this app deals in the two differ
 * by far less than the compass on the phone can tell apart, but the initial bearing
 * is the one that matches an arrow you follow and correct as you walk.
 */
fun Coord.bearingTo(other: Coord): Double {
    val phi1 = lat * PI / 180.0
    val phi2 = other.lat * PI / 180.0
    val dLambda = (other.lon - lon) * PI / 180.0

    val y = sin(dLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
    return (atan2(y, x) * 180.0 / PI + 360.0) % 360.0
}
