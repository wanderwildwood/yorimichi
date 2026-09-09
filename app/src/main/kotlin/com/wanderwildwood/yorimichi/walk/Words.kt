package com.wanderwildwood.yorimichi.walk

import com.wanderwildwood.yorimichi.core.Coord
import java.util.Locale
import kotlin.math.roundToInt

private const val METRES_PER_MILE = 1_609.344
private const val METRES_PER_FOOT = 0.3048

/**
 * How far it is, said the way it would be said out loud.
 *
 * Rounded hard on purpose. The fix under this is good to a few metres on a clear day
 * and worse under trees, and the point itself is an arbitrary cell of a grid, so a
 * distance printed to the metre would be claiming a precision that nothing in the chain
 * has.
 */
fun distance(metres: Double, units: Units): String = when (units) {
    Units.IMPERIAL -> {
        val feet = metres / METRES_PER_FOOT
        if (feet < 1_000) "${(feet / 10).roundToInt() * 10} ft"
        else String.format(Locale.US, "%.1f mi", metres / METRES_PER_MILE)
    }

    Units.METRIC -> {
        if (metres < 1_000) "${(metres / 10).roundToInt() * 10} m"
        else String.format(Locale.US, "%.1f km", metres / 1_000)
    }
}

private val POINTS = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)

/** The sixteen-point compass name for a bearing. */
fun cardinal(degrees: Double): String {
    val normalised = ((degrees % 360.0) + 360.0) % 360.0
    return POINTS[(normalised / 22.5).roundToInt() % 16]
}

/** A bearing as it is written next to the arrow: "NNE 32°". */
fun bearing(degrees: Double): String =
    "${cardinal(degrees)} ${degrees.roundToInt() % 360}°"

/**
 * Five decimal places is a bit over a metre, which is finer than the fix that produced
 * it and finer than anyone can stand in. It is here to be read into another app, so it
 * is written the way another app expects rather than prettily.
 */
fun coordinate(coord: Coord): String =
    String.format(Locale.US, "%.5f, %.5f", coord.lat, coord.lon)

/**
 * What the scatter actually did, in a sentence.
 *
 * The app will always find a thickest cell, and most of the time that cell is nothing
 * much. Saying so is the whole point of measuring it: an app that reports every run as
 * a discovery is not reporting anything.
 */
fun reading(concentration: Double): String = when {
    concentration >= 2.0 ->
        "The points fell ${times(concentration)} times thicker here than an even scatter."

    concentration >= 1.3 -> "A little thicker here than an even scatter."
    concentration > 0.75 -> "No thicker here than chance usually gives."
    concentration > 0.4 -> "A little thinner here than an even scatter."
    else ->
        "The points fell ${times(1.0 / concentration)} times thinner here than an even scatter."
}

private fun times(value: Double): String = String.format(Locale.US, "%.1f", value)

/**
 * What the phone knows about where it is, said plainly.
 *
 * A fix is not a fact, and this line is where the app says so: how well it is known,
 * and how old it is when that matters. Scattering a walk around a position the phone
 * held twenty minutes ago is the failure this exists to make visible.
 */
fun fixLine(fix: com.wanderwildwood.yorimichi.device.Fix?, units: Units): String = when {
    fix == null -> "Waiting for a fix"
    fix.ageMillis > 120_000 -> "Last fix ${age(fix.ageMillis)} ago"
    fix.accuracyMetres == Float.MAX_VALUE -> "Fix of unknown accuracy"
    else -> "Fix good to ${distance(fix.accuracyMetres.toDouble(), units)}"
}

private fun age(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1_440 -> "${minutes / 60} hr"
        else -> "${minutes / 1_440} days"
    }
}
