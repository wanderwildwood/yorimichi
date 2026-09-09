package com.wanderwildwood.yorimichi.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wanderwildwood.yorimichi.core.Coord
import java.util.Locale

/**
 * Handing the point to whatever draws maps on this phone.
 *
 * This is the whole of the app's relationship with maps, and it is deliberate. A `geo:`
 * intent is answered here by HERE WeGo, which carries its maps offline and can navigate
 * to a point, and by c:geo. Either of them draws a better map than this app could, on a
 * panel that would rather not be asked to draw one at all.
 */
fun geoUri(coord: Coord): Uri {
    // Formatted in the US locale on purpose. A locale that writes decimals with a comma
    // turns "35.681,139.767" into "35,681,139,767", which every map app reads as
    // nonsense and none of them complain about.
    val point = String.format(Locale.US, "%.6f,%.6f", coord.lat, coord.lon)
    return Uri.parse("geo:0,0?q=$point(${Uri.encode("Detour")})")
}

/**
 * Whether anything on this phone answers a `geo:` intent.
 *
 * Needs the `<queries>` declaration in the manifest: from Android 11 an app cannot see
 * what else is installed without saying in advance what it means to look for, and
 * without it this quietly answers no on a phone where the map is right there.
 */
fun canOpenMap(context: Context): Boolean =
    Intent(Intent.ACTION_VIEW, geoUri(Coord(0.0, 0.0)))
        .resolveActivity(context.packageManager) != null

fun openMap(context: Context, coord: Coord) {
    val intent = Intent(Intent.ACTION_VIEW, geoUri(coord))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
