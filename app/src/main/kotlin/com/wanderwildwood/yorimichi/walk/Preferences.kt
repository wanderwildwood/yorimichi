package com.wanderwildwood.yorimichi.walk

import android.content.Context

/** Which end of the scatter to be sent to. */
enum class Look { GATHER, THIN }

/** Which units distances are said in. */
enum class Units { IMPERIAL, METRIC }

/** How far a walk may be, and how that distance is said. */
data class Radius(val metres: Int, val label: String)

/**
 * The five walks on offer.
 *
 * Five steps rather than a slider: a slider on this panel is a drag with a redraw
 * behind every pixel of it, and nobody wants 1,340 metres. They are round numbers in
 * whichever units are being used rather than one list converted into the other, because
 * "0.3 miles" is not a distance anyone chooses.
 */
fun radii(units: Units): List<Radius> = when (units) {
    Units.METRIC -> listOf(
        Radius(500, "500 m"),
        Radius(1_000, "1 km"),
        Radius(2_000, "2 km"),
        Radius(5_000, "5 km"),
        Radius(10_000, "10 km"),
    )

    Units.IMPERIAL -> listOf(
        Radius(402, "\u00bc mile"),
        Radius(805, "\u00bd mile"),
        Radius(1_609, "1 mile"),
        Radius(4_828, "3 miles"),
        Radius(8_047, "5 miles"),
    )
}

/** The offered walk nearest [metres], for when the units change under a chosen one. */
fun nearestRadius(metres: Int, units: Units): Radius =
    radii(units).minBy { kotlin.math.abs(it.metres - metres) }

/** The three things there are to set. */
class Preferences(context: Context) {

    private val store = context.getSharedPreferences("detour", Context.MODE_PRIVATE)

    var radiusMetres: Int
        get() = store.getInt(RADIUS, 2_000)
        set(value) = store.edit().putInt(RADIUS, value).apply()

    var look: Look
        get() = Look.valueOf(store.getString(LOOK, Look.GATHER.name) ?: Look.GATHER.name)
        set(value) = store.edit().putString(LOOK, value.name).apply()

    var units: Units
        get() = Units.valueOf(store.getString(UNITS, Units.IMPERIAL.name) ?: Units.IMPERIAL.name)
        set(value) = store.edit().putString(UNITS, value.name).apply()

    private companion object {
        const val RADIUS = "radius"
        const val LOOK = "look"
        const val UNITS = "units"
    }
}
