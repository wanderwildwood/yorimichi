package com.wanderwildwood.yorimichi.walk

import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.core.bearingTo
import com.wanderwildwood.yorimichi.core.distanceTo
import com.wanderwildwood.yorimichi.device.Fix
import kotlin.math.roundToInt

/** Within this many metres of the point, the walk is over and the screen says so. */
const val ARRIVED_METRES = 25.0

/**
 * A position older than this, while walking, is no longer where the phone is. A walker
 * covers well over a hundred metres in it, so the distance shown can no longer be trusted
 * and the screen says so instead of holding the last number.
 */
const val WALKING_STALE_MILLIS = 60_000L

/** A second source (the network) is only allowed to move the position when it is no worse, or the GPS has gone quiet this long. */
const val QUIET_GPS_MILLIS = 15_000L

/** How far it is from where the phone is to the point, and which way, right now. */
data class Leg(
    val distanceMetres: Double,
    /** Degrees clockwise from true north, in whole degrees so the arrow only moves for a real change. */
    val bearingDegrees: Double,
    val arrived: Boolean,
)

/** The leg from [from] to [to]. Recomputed from every fix; never carried over from the start of the walk. */
fun leg(from: Coord, to: Coord): Leg {
    val metres = from.distanceTo(to)
    val bearing = (from.bearingTo(to).roundToInt() % 360).toDouble()
    return Leg(metres, bearing, metres <= ARRIVED_METRES)
}

/** Whether the position [fix] is too old to follow a walker by. */
fun staleWhileWalking(fix: Fix?, now: Long): Boolean =
    fix == null || fix.ageMillis(now) > WALKING_STALE_MILLIS

/**
 * Whether [incoming] should replace [current]. Several providers report at once and a
 * coarse network position must not drag the arrow back from a good GPS one.
 */
fun accept(current: Fix?, incoming: Fix): Boolean = when {
    current == null -> true
    incoming.timeMillis < current.timeMillis -> false
    incoming.accuracyMetres <= current.accuracyMetres -> true
    else -> incoming.timeMillis - current.timeMillis >= QUIET_GPS_MILLIS
}
