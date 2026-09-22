package com.wanderwildwood.yorimichi.walk

import androidx.annotation.StringRes
import com.wanderwildwood.yorimichi.R
import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.device.Fix
import java.util.Locale
import kotlin.math.roundToInt

private const val METRES_PER_MILE = 1_609.344
private const val METRES_PER_FOOT = 0.3048

/**
 * A distance in the units it is said in, rounded to what is worth saying. The screen
 * puts the words to it.
 *
 * Short distances are whole tens of feet or metres. Long ones are carried as they are
 * and said to one decimal place, by [oneDecimal].
 */
sealed interface Distance {
    data class Feet(val feet: Int) : Distance
    data class Miles(val miles: Double) : Distance
    data class Metres(val metres: Int) : Distance
    data class Kilometres(val kilometres: Double) : Distance
}

/**
 * How far it is, in the units it would be said in out loud.
 *
 * Rounded hard on purpose. The fix under this is good to a few metres on a clear day
 * and worse under trees, and the point itself is an arbitrary cell of a grid, so a
 * distance printed to the metre would be claiming a precision that nothing in the chain
 * has.
 */
fun distance(metres: Double, units: Units): Distance = when (units) {
    Units.IMPERIAL -> {
        val feet = metres / METRES_PER_FOOT
        if (feet < 1_000) Distance.Feet((feet / 10).roundToInt() * 10)
        else Distance.Miles(metres / METRES_PER_MILE)
    }

    Units.METRIC -> {
        if (metres < 1_000) Distance.Metres((metres / 10).roundToInt() * 10)
        else Distance.Kilometres(metres / 1_000)
    }
}

/**
 * One decimal place, in Western digits whatever language the phone is in. Every number
 * on the screen has always been written this way, and changing that is its own decision.
 */
fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

/** The sixteen points of the compass, clockwise from north, and the word for each. */
enum class CompassPoint(@StringRes val labelRes: Int) {
    N(R.string.compass_n), NNE(R.string.compass_nne), NE(R.string.compass_ne), ENE(R.string.compass_ene),
    E(R.string.compass_e), ESE(R.string.compass_ese), SE(R.string.compass_se), SSE(R.string.compass_sse),
    S(R.string.compass_s), SSW(R.string.compass_ssw), SW(R.string.compass_sw), WSW(R.string.compass_wsw),
    W(R.string.compass_w), WNW(R.string.compass_wnw), NW(R.string.compass_nw), NNW(R.string.compass_nnw),
}

/** The sixteen-point compass point for a bearing. */
fun cardinal(degrees: Double): CompassPoint {
    val normalised = ((degrees % 360.0) + 360.0) % 360.0
    return CompassPoint.entries[(normalised / 22.5).roundToInt() % 16]
}

/** A bearing as it is written next to the arrow, a point and whole degrees: "NNE 32°". */
data class Bearing(val point: CompassPoint, val degrees: Int)

fun bearing(degrees: Double): Bearing =
    Bearing(cardinal(degrees), degrees.roundToInt() % 360)

/**
 * Five decimal places is a bit over a metre, which is finer than the fix that produced
 * it and finer than anyone can stand in. It is here to be read into another app, so it
 * is written the way another app expects rather than prettily.
 */
fun coordinate(coord: Coord): String =
    String.format(Locale.US, "%.5f, %.5f", coord.lat, coord.lon)

/** What the scatter did, as one of five things to say about it. */
sealed interface Reading {
    /** At least twice an even scatter, and how many times. */
    data class Thicker(val times: Double) : Reading
    data object LittleThicker : Reading
    data object Ordinary : Reading
    data object LittleThinner : Reading

    /** Well under half an even scatter, and how many times thinner. */
    data class Thinner(val times: Double) : Reading
}

/**
 * What the scatter actually did.
 *
 * The app will always find a thickest cell, and most of the time that cell is nothing
 * much. Saying so is the whole point of measuring it: an app that reports every run as
 * a discovery is not reporting anything.
 */
fun reading(concentration: Double): Reading = when {
    concentration >= 2.0 -> Reading.Thicker(concentration)
    concentration >= 1.3 -> Reading.LittleThicker
    concentration > 0.75 -> Reading.Ordinary
    concentration > 0.4 -> Reading.LittleThinner
    else -> Reading.Thinner(1.0 / concentration)
}

/** The line under the walk that says what the phone knows about where it is. */
sealed interface FixLine {
    data object Waiting : FixLine

    /** Old enough that its age is the thing worth saying. */
    data class Stale(val age: Age) : FixLine
    data object UnknownAccuracy : FixLine
    data class GoodTo(val accuracy: Distance) : FixLine
}

/**
 * What the phone knows about where it is.
 *
 * A fix is not a fact, and this line is where the app says so: how well it is known,
 * and how old it is when that matters. Scattering a walk around a position the phone
 * held twenty minutes ago is the failure this exists to make visible.
 */
fun fixLine(fix: Fix?, units: Units): FixLine = when {
    fix == null -> FixLine.Waiting
    fix.ageMillis > 120_000 -> FixLine.Stale(age(fix.ageMillis))
    fix.accuracyMetres == Float.MAX_VALUE -> FixLine.UnknownAccuracy
    else -> FixLine.GoodTo(distance(fix.accuracyMetres.toDouble(), units))
}

/** How old something is, in the largest whole unit that is not zero. */
sealed interface Age {
    data class Minutes(val minutes: Long) : Age
    data class Hours(val hours: Long) : Age
    data class Days(val days: Long) : Age
}

fun age(millis: Long): Age {
    val minutes = millis / 60_000
    return when {
        minutes < 60 -> Age.Minutes(minutes)
        minutes < 1_440 -> Age.Hours(minutes / 60)
        else -> Age.Days(minutes / 1_440)
    }
}
