package com.wanderwildwood.yorimichi.walk

import android.content.Context
import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.device.Fix

/** Which end of the scatter to be sent to. */
enum class Look { GATHER, THIN }

/** Which units distances are said in. */
enum class Units { IMPERIAL, METRIC }

/**
 * Miles and feet where the country uses them (the United States, the United Kingdom, Liberia
 * and Myanmar), kilometres and metres everywhere else. Only the first-run choice: the Settings
 * row changes it and the choice is kept.
 */
fun unitsForCountry(country: String?): Units = when (country?.uppercase()) {
    "US", "GB", "LR", "MM" -> Units.IMPERIAL
    else -> Units.METRIC
}

/** How far a walk may be, and how that distance is said. */
data class Radius(val metres: Int, val label: RadiusLabel)

/**
 * A walk's distance as it is offered: a round number in its own units, or one of the
 * two fractions of a mile that have words of their own. The screen puts the words to it.
 */
sealed interface RadiusLabel {
    data class Metres(val metres: Int) : RadiusLabel
    data class Kilometres(val kilometres: Int) : RadiusLabel
    data class Miles(val miles: Int) : RadiusLabel
    data object QuarterMile : RadiusLabel
    data object HalfMile : RadiusLabel
}

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
        Radius(500, RadiusLabel.Metres(500)),
        Radius(1_000, RadiusLabel.Kilometres(1)),
        Radius(2_000, RadiusLabel.Kilometres(2)),
        Radius(5_000, RadiusLabel.Kilometres(5)),
        Radius(10_000, RadiusLabel.Kilometres(10)),
    )

    Units.IMPERIAL -> listOf(
        Radius(402, RadiusLabel.QuarterMile),
        Radius(805, RadiusLabel.HalfMile),
        Radius(1_609, RadiusLabel.Miles(1)),
        Radius(4_828, RadiusLabel.Miles(3)),
        Radius(8_047, RadiusLabel.Miles(5)),
    )
}

/** The offered walk nearest [metres], for when the units change under a chosen one. */
fun nearestRadius(metres: Int, units: Units): Radius =
    radii(units).minBy { kotlin.math.abs(it.metres - metres) }

/** The three things there are to set, and the last place the phone was. */
class Preferences(context: Context) {

    private val store = context.getSharedPreferences("detour", Context.MODE_PRIVATE)

    var radiusMetres: Int
        get() = store.getInt(RADIUS, 2_000)
        set(value) = store.edit().putInt(RADIUS, value).apply()

    var look: Look
        get() = Look.valueOf(store.getString(LOOK, Look.GATHER.name) ?: Look.GATHER.name)
        set(value) = store.edit().putString(LOOK, value.name).apply()

    var units: Units
        get() = store.getString(UNITS, null)?.let { Units.valueOf(it) }
            ?: unitsForCountry(java.util.Locale.getDefault().country)
        set(value) = store.edit().putString(UNITS, value.name).apply()

    /**
     * The last fresh fix, one only, overwritten each time.
     *
     * Kept because the GPS forgets across a restart, and indoors it may not find the sky
     * again for a long time: with this the app can still start from where the phone last
     * was, and say how long ago that was. It stays in the app's private storage, which is
     * not backed up, and the app has no way to send it anywhere.
     */
    var lastFix: Fix?
        get() {
            if (!store.contains(LAST_TIME)) return null
            return Fix(
                coord = Coord(
                    Double.fromBits(store.getLong(LAST_LAT, 0)),
                    Double.fromBits(store.getLong(LAST_LON, 0)),
                ),
                accuracyMetres = store.getFloat(LAST_ACCURACY, Float.MAX_VALUE),
                timeMillis = store.getLong(LAST_TIME, 0),
            )
        }
        set(value) {
            val edit = store.edit()
            if (value == null) {
                edit.remove(LAST_LAT).remove(LAST_LON).remove(LAST_ACCURACY).remove(LAST_TIME)
            } else {
                edit.putLong(LAST_LAT, value.coord.lat.toRawBits())
                    .putLong(LAST_LON, value.coord.lon.toRawBits())
                    .putFloat(LAST_ACCURACY, value.accuracyMetres)
                    .putLong(LAST_TIME, value.timeMillis)
            }
            edit.apply()
        }

    /** Whether the first-run safety notice has been read. */
    var safetySeen: Boolean
        get() = store.getBoolean(SAFETY_SEEN, false)
        set(value) = store.edit().putBoolean(SAFETY_SEEN, value).apply()

    private companion object {
        const val SAFETY_SEEN = "safety_seen"
        const val RADIUS = "radius"
        const val LOOK = "look"
        const val UNITS = "units"
        const val LAST_LAT = "last_lat"
        const val LAST_LON = "last_lon"
        const val LAST_ACCURACY = "last_accuracy"
        const val LAST_TIME = "last_time"
    }
}
