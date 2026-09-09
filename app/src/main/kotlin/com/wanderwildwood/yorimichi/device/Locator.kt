package com.wanderwildwood.yorimichi.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.wanderwildwood.yorimichi.core.Coord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Where the phone thinks it is, and how well it knows. */
data class Fix(
    val coord: Coord,
    val accuracyMetres: Float,
    /** How long ago the fix was taken. */
    val ageMillis: Long,
) {
    /** Good enough to scatter points around. */
    val usable: Boolean get() = accuracyMetres <= 50f && ageMillis < 120_000
}

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

/**
 * A stream of fixes from the GPS.
 *
 * The GPS provider only, deliberately. The network provider on a phone with no Google
 * services behind it has nothing to answer with, so registering for it is registering
 * for a callback that never comes, and taking its last known fix means taking one from
 * whenever the phone last had a different set of software on it.
 *
 * The last known GPS fix goes out first anyway, stale or not, because a stale fix with
 * its age shown beats an empty screen while the sky is being found — and the age is
 * shown, so a walk is never scattered around somewhere the phone was yesterday without
 * saying so.
 */
@SuppressLint("MissingPermission")
fun fixes(context: Context): Flow<Fix> = callbackFlow {
    if (!hasLocationPermission(context)) {
        close()
        return@callbackFlow
    }

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun send(location: Location?) {
        if (location == null) return
        val age = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
        trySend(
            Fix(
                coord = Coord(location.latitude, location.longitude),
                accuracyMetres = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
                ageMillis = age.coerceAtLeast(0),
            )
        )
    }

    send(runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull())

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) = send(location)

        // Deprecated on API 29 and up and never called there, but the interface still
        // declares it on the SDK this builds against.
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }

    runCatching {
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, listener)
    }

    awaitClose { manager.removeUpdates(listener) }
}
