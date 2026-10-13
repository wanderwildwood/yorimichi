package com.wanderwildwood.yorimichi.device

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.flow.distinctUntilChanged

/** Where the phone thinks it is, how well it knows, and since when. */
data class Fix(
    val coord: Coord,
    val accuracyMetres: Float,
    /** When the fix was taken, by the wall clock, so that it still means something after a restart. */
    val timeMillis: Long,
) {
    fun ageMillis(now: Long): Long = (now - timeMillis).coerceAtLeast(0)
}

/** A fix is fresh for two minutes. After that it is somewhere the phone was. */
const val FRESH_MILLIS = 120_000L

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

/** Whether location is switched on for the whole phone, in the system's settings or the shade. */
fun locationOn(context: Context): Boolean =
    (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled

/** The location switch, now and every time it is flipped, from the settings or the shade. */
fun locationSwitch(context: Context): Flow<Boolean> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(locationOn(context))
        }
    }
    trySend(locationOn(context))
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter(LocationManager.MODE_CHANGED_ACTION),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    awaitClose { context.unregisterReceiver(receiver) }
}.distinctUntilChanged()

private fun Location.toFix(): Fix {
    val age = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000
    return Fix(
        coord = Coord(latitude, longitude),
        accuracyMetres = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
        timeMillis = System.currentTimeMillis() - age.coerceAtLeast(0),
    )
}

/**
 * The newest position any provider on the phone still remembers, however old.
 *
 * Every provider is asked, not just the GPS: only the GPS is listened to for a fresh fix
 * (see [fixes]), but a position the phone already holds costs nothing to read, and a
 * stale one shown with its age is far better than a screen that waits. The age is always
 * said, so a walk is never scattered around somewhere the phone was yesterday without the
 * screen saying so.
 */
@SuppressLint("MissingPermission")
fun lastKnown(context: Context): Fix? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return runCatching { manager.allProviders }.getOrDefault(emptyList())
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .map { it.toFix() }
        .maxByOrNull { it.timeMillis }
}

/**
 * A stream of fresh fixes from the GPS.
 *
 * The GPS provider only, deliberately. The network provider on a phone with no Google
 * services behind it has nothing to answer with, so registering for it is registering
 * for a callback that never comes. The Kompakt has the GPS and nothing else.
 *
 * The registration outlives the location switch: turned off, the GPS falls silent, and
 * turned back on it starts answering this same listener again.
 */
@SuppressLint("MissingPermission")
fun fixes(context: Context): Flow<Fix> = callbackFlow {
    if (!hasLocationPermission(context)) {
        close()
        return@callbackFlow
    }

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            trySend(location.toFix())
        }

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
