package com.wanderwildwood.yorimichi.walk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanderwildwood.yorimichi.core.Attractor
import com.wanderwildwood.yorimichi.core.Selector
import com.wanderwildwood.yorimichi.core.generate
import com.wanderwildwood.yorimichi.device.FRESH_MILLIS
import com.wanderwildwood.yorimichi.device.Fix
import com.wanderwildwood.yorimichi.device.declination
import com.wanderwildwood.yorimichi.device.fixes
import com.wanderwildwood.yorimichi.device.hasCompass
import com.wanderwildwood.yorimichi.device.hasLocationPermission
import com.wanderwildwood.yorimichi.device.lastKnown
import com.wanderwildwood.yorimichi.device.locationOn
import com.wanderwildwood.yorimichi.device.locationSwitch
import com.wanderwildwood.yorimichi.device.magneticHeadings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

/** Everything the one screen needs to draw itself. */
data class WalkState(
    val permitted: Boolean = false,
    /**
     * The permission was refused and Android will not ask again, so the only way left
     * is the app's page in the system settings.
     */
    val refusedForGood: Boolean = false,
    /** Location switched on for the whole phone. */
    val locationOn: Boolean = true,
    /** The newest position known: fresh from the GPS, or remembered, with its time. */
    val fix: Fix? = null,
    /** When the GPS started listening with location on, or null while it cannot. */
    val searchingSince: Long? = null,
    /** The clock the screen reads ages against, moved on every [TICK_MILLIS]. */
    val now: Long = System.currentTimeMillis(),
    /** Degrees clockwise from **true** north, or null where there is no compass to ask. */
    val heading: Float? = null,
    val hasCompass: Boolean = false,
    val attractor: Attractor? = null,
    val radiusMetres: Int = 2_000,
    val look: Look = Look.GATHER,
    val units: Units = Units.IMPERIAL,
)

/**
 * How many points the scatter is made of.
 *
 * A thousand is enough for the density surface to mean something and few enough that
 * the whole calculation is over before a finger leaves the button. It is not a setting:
 * nobody has any way to prefer one number here to another.
 */
private const val POINTS = 1_024

/**
 * How often the screen's clock moves on, so that an age goes on counting and the
 * "still looking" line turns into "not found" while nothing else happens. Ages are said
 * in whole minutes, so nothing finer is ever visible.
 */
private const val TICK_MILLIS = 30_000L

/** How often a fresh fix is written down. It changes every second while walking. */
private const val SAVE_EVERY_MILLIS = 60_000L

class WalkViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = Preferences(application)

    private val _state = MutableStateFlow(
        WalkState(
            permitted = hasLocationPermission(application),
            hasCompass = hasCompass(application),
            // Snapped to one of the walks the current units actually offer. The stored
            // value can be a metric one under imperial units after a change of mind, or
            // the very first time, and a walk that is not on the list has no name to
            // show - it came out as "Within 2000 m" under a screen that speaks miles.
            radiusMetres = nearestRadius(preferences.radiusMetres, preferences.units).metres,
            look = preferences.look,
            units = preferences.units,
        )
    )
    val state = _state.asStateFlow()

    private var watching: Job? = null
    private var savedAt = 0L

    init {
        if (_state.value.permitted) watch()
    }

    /**
     * Called when the permission dialog has been answered. [canAskAgain] is false when
     * Android has stopped showing the dialog, after a second refusal or a "don't ask".
     */
    fun permissionAnswered(granted: Boolean, canAskAgain: Boolean) {
        _state.update { it.copy(permitted = granted, refusedForGood = !granted && !canAskAgain) }
        if (granted) watch()
    }

    /** Back on screen, perhaps from the system settings: look again at what may have changed. */
    fun resumed() {
        val app = getApplication<Application>()
        val permitted = hasLocationPermission(app)
        _state.update {
            it.copy(
                permitted = permitted,
                refusedForGood = it.refusedForGood && !permitted,
                now = System.currentTimeMillis(),
            )
        }
        if (permitted) watch()
    }

    private fun watch() {
        if (watching != null) return
        val app = getApplication<Application>()

        // Something to start from at once, before the sky has been asked anything: the
        // newest of whatever the phone still remembers and what this app wrote down last
        // time. The screen says how old it is.
        val remembered = listOfNotNull(lastKnown(app), preferences.lastFix).maxByOrNull { it.timeMillis }
        val now = System.currentTimeMillis()
        _state.update {
            it.copy(
                fix = remembered,
                locationOn = locationOn(app),
                searchingSince = if (locationOn(app)) now else null,
                now = now,
            )
        }

        watching = viewModelScope.launch {
            launch {
                locationSwitch(app).collect { on ->
                    _state.update {
                        it.copy(
                            locationOn = on,
                            // The wait starts over each time it is switched on.
                            searchingSince = when {
                                !on -> null
                                it.searchingSince == null -> System.currentTimeMillis()
                                else -> it.searchingSince
                            },
                        )
                    }
                }
            }
            launch {
                fixes(app).collect { fix ->
                    _state.update { it.copy(fix = fix, now = System.currentTimeMillis()) }
                    remember(fix)
                }
            }
            launch {
                while (true) {
                    delay(TICK_MILLIS)
                    _state.update { it.copy(now = System.currentTimeMillis()) }
                }
            }
            launch {
                magneticHeadings(getApplication()).collect { magnetic ->
                    _state.update { current ->
                        // True north, so that the arrow and the bearing agree. The
                        // correction needs a position, so until there is a fix the
                        // heading is left as it comes and is out by the local skew.
                        val correction = current.fix
                            ?.let { declination(it.coord, System.currentTimeMillis()) }
                            ?: 0f
                        current.copy(heading = (magnetic + correction + 360f) % 360f)
                    }
                }
            }
        }
    }

    /** Write a fresh fix down, now and then, for the next time the sky cannot be found. */
    private fun remember(fix: Fix) {
        if (fix.ageMillis(System.currentTimeMillis()) > FRESH_MILLIS) return
        if (fix.timeMillis - savedAt < SAVE_EVERY_MILLIS) return
        preferences.lastFix = fix
        savedAt = fix.timeMillis
    }

    /** Pick a place to go. */
    fun go() {
        val fix = _state.value.fix ?: return
        val selector = when (_state.value.look) {
            Look.GATHER -> Selector.Densest
            Look.THIN -> Selector.Emptiest
        }
        // The device's own generator, seeded from the kernel's entropy pool. Nothing is
        // fetched: an app that asks a server for its randomness has told that server
        // where its user is about to walk.
        val random = SecureRandom().asKotlinRandom()
        val attractor = generate(
            origin = fix.coord,
            radiusMetres = _state.value.radiusMetres.toDouble(),
            pointCount = POINTS,
            selector = selector,
            random = random,
        )
        _state.update { it.copy(attractor = attractor) }
    }

    /** Put the walk away and go back to the button. */
    fun clear() = _state.update { it.copy(attractor = null) }

    fun nextRadius() {
        val offered = radii(_state.value.units)
        val here = offered.indexOfFirst { it.metres == _state.value.radiusMetres }
        val next = offered[(here + 1) % offered.size]
        setRadius(next.metres)
    }

    fun toggleLook() {
        val next = if (_state.value.look == Look.GATHER) Look.THIN else Look.GATHER
        preferences.look = next
        _state.update { it.copy(look = next) }
    }

    fun toggleUnits() {
        val next = if (_state.value.units == Units.IMPERIAL) Units.METRIC else Units.IMPERIAL
        preferences.units = next
        _state.update { it.copy(units = next) }
        // The two lists of walks do not line up, so the chosen one moves to whichever
        // of the new list it is nearest. A mile stays a mile and does not become 1,609.
        setRadius(nearestRadius(_state.value.radiusMetres, next).metres)
    }

    private fun setRadius(metres: Int) {
        preferences.radiusMetres = metres
        _state.update { it.copy(radiusMetres = metres) }
    }
}
