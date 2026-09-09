package com.wanderwildwood.yorimichi.walk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanderwildwood.yorimichi.core.Attractor
import com.wanderwildwood.yorimichi.core.Selector
import com.wanderwildwood.yorimichi.core.generate
import com.wanderwildwood.yorimichi.device.Fix
import com.wanderwildwood.yorimichi.device.declination
import com.wanderwildwood.yorimichi.device.fixes
import com.wanderwildwood.yorimichi.device.hasCompass
import com.wanderwildwood.yorimichi.device.hasLocationPermission
import com.wanderwildwood.yorimichi.device.magneticHeadings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

/** Everything the one screen needs to draw itself. */
data class WalkState(
    val permitted: Boolean = false,
    val fix: Fix? = null,
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

    init {
        if (_state.value.permitted) watch()
    }

    /** Called when the permission dialog has been answered. */
    fun permissionAnswered(granted: Boolean) {
        _state.update { it.copy(permitted = granted) }
        if (granted) watch()
    }

    private fun watch() {
        if (watching != null) return
        watching = viewModelScope.launch {
            launch {
                fixes(getApplication()).collect { fix ->
                    _state.update { it.copy(fix = fix) }
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
