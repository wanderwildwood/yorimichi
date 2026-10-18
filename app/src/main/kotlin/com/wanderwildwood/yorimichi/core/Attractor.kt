package com.wanderwildwood.yorimichi.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Picking a place to walk to.
 *
 * Scatter points uniformly over the disc around where you are, estimate how thickly
 * they fell across it, and hand back one cell of that estimate. A thick cell is what
 * the Fatum papers call an attractor and a thin one a void; the arithmetic is the
 * same either way and only the choice at the end differs.
 *
 * What this is not: the numbers come from the device, so nothing here is quantum and
 * nothing here has been shown to know anything. The scatter is random and the density
 * surface is real; any meaning past that is the walker's, not the app's.
 */

/** Which part of the density surface to send the walker to. */
sealed interface Selector {
    /** The thickest cell: an attractor. */
    data object Densest : Selector

    /** The thinnest cell: a void. */
    data object Emptiest : Selector

    /** A cell [fraction] of the way up the sorted densities; 0.5 is the median. */
    data class Quantile(val fraction: Double) : Selector
}

/** Where to go, and what the surface looked like there. */
data class Attractor(
    val point: Coord,
    val distanceMetres: Double,
    val bearingDegrees: Double,
    /**
     * How far the scatter's density at the chosen cell stands above (or below, when negative)
     * what an even scatter would give there, in units of how far chance strays there. This is
     * what the cells are ranked by. Comparable only within one run.
     */
    val density: Double,
    /**
     * How much thicker the scatter is around the chosen point than an even scatter
     * over the whole disc would be. One means ordinary: the points fell around this
     * spot about as they would anywhere. Two means twice as thick. Below one is a
     * thin patch, which is what [Selector.Emptiest] is looking for.
     *
     * This is the number that keeps the app honest. A run will always return a
     * densest cell, and most of the time that cell is unremarkable; without this
     * there is no way to say so.
     */
    val concentration: Double,
    val points: Int,
    val radiusMetres: Double,
)

/** The density surface is estimated on a square grid this many cells on a side. */
const val GRID: Int = 100

/**
 * Scatter [pointCount] points inside [radiusMetres] of [origin], estimate their
 * density, and return the cell [selector] asks for.
 *
 * Everything between the scatter and the answer happens in metres east and north of
 * [origin] rather than in degrees. Degrees are not square — a degree of longitude is
 * shorter than a degree of latitude everywhere but the equator — so a kernel that is
 * round in degrees is an ellipse on the ground, and the distortion grows with
 * latitude. Both projects this is descended from work in degrees and carry it.
 */
fun generate(
    origin: Coord,
    radiusMetres: Double,
    pointCount: Int,
    selector: Selector,
    random: Random,
): Attractor {
    require(radiusMetres > 0) { "radius must be positive" }
    require(pointCount >= 2) { "need at least two points to estimate a density" }

    val (east, north) = scatter(radiusMetres, pointCount, random)

    val bandwidthEast = silvermanBandwidth(east)
    val bandwidthNorth = silvermanBandwidth(north)

    var minEast = east[0]; var maxEast = east[0]
    var minNorth = north[0]; var maxNorth = north[0]
    for (i in 1 until pointCount) {
        if (east[i] < minEast) minEast = east[i]
        if (east[i] > maxEast) maxEast = east[i]
        if (north[i] < minNorth) minNorth = north[i]
        if (north[i] > maxNorth) maxNorth = north[i]
    }

    // A Gaussian kernel over a GRID x GRID lattice spanning the points. The kernel
    // separates — exp(-(u² + v²)/2) is exp(-u²/2) times exp(-v²/2) — so each axis is
    // exponentiated once per cell per point and the two are multiplied, rather than
    // calling exp() on the pair. That is one exp() where the obvious loop wants two.
    val cellEast = DoubleArray(GRID) { minEast + (maxEast - minEast) * it / (GRID - 1.0) }
    val cellNorth = DoubleArray(GRID) { minNorth + (maxNorth - minNorth) * it / (GRID - 1.0) }

    val weightEast = Array(GRID) { cell ->
        DoubleArray(pointCount) { p ->
            val u = (cellEast[cell] - east[p]) / bandwidthEast
            exp(-0.5 * u * u)
        }
    }
    val weightNorth = Array(GRID) { cell ->
        DoubleArray(pointCount) { p ->
            val v = (cellNorth[cell] - north[p]) / bandwidthNorth
            exp(-0.5 * v * v)
        }
    }

    val densities = DoubleArray(GRID * GRID)
    for (i in 0 until GRID) {
        val we = weightEast[i]
        for (j in 0 until GRID) {
            val wn = weightNorth[j]
            var sum = 0.0
            for (p in 0 until pointCount) sum += we[p] * wn[p]
            densities[i * GRID + j] = sum
        }
    }

    // What an even scatter would put on each cell, and how much that wanders from run to run,
    // worked out the same way from points laid evenly over the disc by rule rather than by
    // chance. Near the rim a cell's kernel hangs half outside the disc, where no point can
    // fall, so every cell there reads thin; ranked raw, the voids all sat on the rim and the
    // attractors near the middle. Ranked against this, a place anywhere in the disc is about as
    // likely as any other to be picked.
    val (evenEast, evenNorth) = evenly(radiusMetres, EVEN_POINTS)
    val evenWeightEast = Array(GRID) { cell ->
        DoubleArray(EVEN_POINTS) { p ->
            val u = (cellEast[cell] - evenEast[p]) / bandwidthEast
            exp(-0.5 * u * u)
        }
    }
    val evenWeightNorth = Array(GRID) { cell ->
        DoubleArray(EVEN_POINTS) { p ->
            val v = (cellNorth[cell] - evenNorth[p]) / bandwidthNorth
            exp(-0.5 * v * v)
        }
    }
    val scale = pointCount.toDouble() / EVEN_POINTS
    val relative = DoubleArray(GRID * GRID)
    for (i in 0 until GRID) {
        val we = evenWeightEast[i]
        for (j in 0 until GRID) {
            val wn = evenWeightNorth[j]
            var sum = 0.0
            var squares = 0.0
            for (p in 0 until EVEN_POINTS) {
                val w = we[p] * wn[p]
                sum += w
                squares += w * w
            }
            // How far this cell is from what chance would put there, in units of how far
            // chance itself strays there: thin rim cells stray most, and judged by the raw
            // gap they would still take most of the extremes.
            relative[i * GRID + j] = (densities[i * GRID + j] - sum * scale) / sqrt(max(squares * scale, 1e-300))
        }
    }

    // Only the cells inside the disc are candidates. The lattice is square, spanning the
    // points, and its corners reach out to the radius times the square root of two; no points
    // fall there, so they are the thinnest cells of all, and "where they thin out" sent the
    // walker to them, 4.2 miles out on a 3 mile walk.
    val radiusSquared = radiusMetres * radiusMetres
    val inside = (0 until GRID * GRID).filter { cell ->
        val e = cellEast[cell / GRID]
        val n = cellNorth[cell % GRID]
        e * e + n * n <= radiusSquared
    }
    val chosen = choose(relative, inside, selector)
    val chosenEast = cellEast[chosen / GRID]
    val chosenNorth = cellNorth[chosen % GRID]
    val point = origin.offset(chosenEast, chosenNorth)

    return Attractor(
        point = point,
        distanceMetres = origin.distanceTo(point),
        bearingDegrees = origin.bearingTo(point),
        density = relative[chosen],
        concentration = concentration(east, north, chosenEast, chosenNorth, radiusMetres),
        points = pointCount,
        radiusMetres = radiusMetres,
    )
}

/**
 * How many points fall near the chosen spot, over how many an even scatter would put
 * there.
 *
 * Measured from the [NEIGHBOURS]th nearest point rather than inside a radius fixed in
 * advance, so it adapts to whatever scale the cluster happens to have: k points inside
 * r means a local density of k / pi r squared, against N / pi R squared for the disc,
 * and the pis divide out.
 */
private fun concentration(
    east: DoubleArray,
    north: DoubleArray,
    atEast: Double,
    atNorth: Double,
    radiusMetres: Double,
): Double {
    val n = east.size
    val k = minOf(NEIGHBOURS, n - 1).coerceAtLeast(1)

    val squared = DoubleArray(n) { i ->
        val de = east[i] - atEast
        val dn = north[i] - atNorth
        de * de + dn * dn
    }
    squared.sort()

    // Floored at a centimetre squared: k points in no area at all is an infinity, and
    // an infinity on the screen says less than a large number does.
    val kth = max(squared[k - 1], 0.0001)
    return (k * radiusMetres * radiusMetres) / (n * kth)
}

/** How many neighbours the local density is measured over. */
private const val NEIGHBOURS = 10

/**
 * [pointCount] points spread evenly over the disc of [radiusMetres], as metres east
 * and north of its centre.
 *
 * Evenly over the *area*, which is what needs the square root. Drawing the radius
 * straight from a uniform number piles the points up in the middle, because a ring far
 * out holds more ground than a ring near the centre and has to be given proportionally
 * more of them. Getting this wrong produces an app that still works, still returns a
 * point, and is quietly biased towards your own doorstep — which is the one thing it
 * exists not to do.
 */
internal fun scatter(
    radiusMetres: Double,
    pointCount: Int,
    random: Random,
): Pair<DoubleArray, DoubleArray> {
    val east = DoubleArray(pointCount)
    val north = DoubleArray(pointCount)
    for (i in 0 until pointCount) {
        val r = radiusMetres * sqrt(random.nextDouble())
        val theta = random.nextDouble() * 2.0 * PI
        east[i] = r * cos(theta)
        north[i] = r * sin(theta)
    }
    return east to north
}

/** How many points [evenly] lays over the disc to say what an even scatter looks like. */
private const val EVEN_POINTS = 1024

/**
 * [count] points laid evenly over the disc of [radiusMetres] by rule, as metres east and
 * north: a sunflower spiral, each a golden angle round from the last and out by the square
 * root, so equal areas hold equal numbers of points with no clumps and no gaps.
 */
internal fun evenly(radiusMetres: Double, count: Int): Pair<DoubleArray, DoubleArray> {
    val golden = PI * (3.0 - sqrt(5.0))
    val east = DoubleArray(count)
    val north = DoubleArray(count)
    for (i in 0 until count) {
        val r = radiusMetres * sqrt((i + 0.5) / count)
        val theta = i * golden
        east[i] = r * cos(theta)
        north[i] = r * sin(theta)
    }
    return east to north
}

/** Index into [densities] of the cell [selector] asks for, from the [candidates] only. */
private fun choose(densities: DoubleArray, candidates: List<Int>, selector: Selector): Int = when (selector) {
    Selector.Densest -> candidates.maxBy { densities[it] }
    Selector.Emptiest -> candidates.minBy { densities[it] }
    is Selector.Quantile -> {
        require(selector.fraction in 0.0..1.0) { "quantile must be between 0 and 1" }
        // Rank the cells and take the one that far up. Sorting the indices rather than
        // the densities keeps the cell's position, which is the part we came for.
        val ranked = candidates.sortedBy { densities[it] }
        ranked[((ranked.size - 1) * selector.fraction).toInt()]
    }
}

/**
 * Silverman's rule for a two-dimensional sample: n^(-1/(d+4)) times the spread.
 *
 * Floored well below a metre so that a degenerate sample — every point in almost the
 * same place, which a tiny radius can produce — cannot divide by zero.
 */
private fun silvermanBandwidth(values: DoubleArray): Double {
    val n = values.size
    val mean = values.sum() / n
    var variance = 0.0
    for (v in values) variance += (v - mean) * (v - mean)
    val deviation = sqrt(variance / n)
    return max(n.toDouble().pow(-1.0 / 6.0) * deviation, 0.01)
}
