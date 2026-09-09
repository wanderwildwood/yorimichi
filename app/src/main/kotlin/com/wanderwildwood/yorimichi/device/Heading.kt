package com.wanderwildwood.yorimichi.device

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.wanderwildwood.yorimichi.core.Coord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/** Whether this phone can say which way it is facing at all. */
fun hasCompass(context: Context): Boolean {
    val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    return sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null ||
        sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null
}

/**
 * Which way the top of the phone is pointing, in degrees clockwise from **magnetic**
 * north. The declination is added later, where the fix is known.
 *
 * Reported in [STEP] degree steps rather than continuously. The arrow this drives is on
 * an e-ink panel that repaints in full, so a heading that updates on every sensor
 * sample is a screen that is always mid-refresh: a smear to look at, a cost to the
 * battery, and no more information than a step gives. Five degrees is under the width
 * of the arrowhead.
 *
 * The screen is locked to portrait, which is why the rotation matrix is used as it
 * comes: there is no display rotation to unwind.
 */
fun magneticHeadings(context: Context): Flow<Float> = callbackFlow {
    val sensors = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    if (sensor == null) {
        close()
        return@callbackFlow
    }

    val rotation = FloatArray(9)
    val orientation = FloatArray(3)
    var last: Float? = null

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            SensorManager.getRotationMatrixFromVector(rotation, event.values)
            SensorManager.getOrientation(rotation, orientation)

            val degrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
            val stepped = (Math.round(degrees / STEP) * STEP) % 360f

            val previous = last
            if (previous == null || turn(previous, stepped) >= STEP) {
                last = stepped
                trySend(stepped)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    awaitClose { sensors.unregisterListener(listener) }
}

/** The heading step, in degrees. */
const val STEP: Float = 5f

/** The smaller of the two ways round from [from] to [to], in degrees. */
fun turn(from: Float, to: Float): Float {
    val difference = abs(to - from) % 360f
    return if (difference > 180f) 360f - difference else difference
}

/**
 * How far magnetic north is from true north at [where], in degrees.
 *
 * Without this the arrow is wrong by however much the local field is skewed — around
 * eight degrees in the eastern United States, which over a two kilometre walk puts the
 * far end nearly three hundred metres off. The model is on the device and needs no
 * network.
 */
fun declination(where: Coord, timeMillis: Long): Float =
    GeomagneticField(
        where.lat.toFloat(),
        where.lon.toFloat(),
        0f,
        timeMillis,
    ).declination
