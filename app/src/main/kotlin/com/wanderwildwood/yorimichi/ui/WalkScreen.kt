package com.wanderwildwood.yorimichi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.walk.Look
import com.wanderwildwood.yorimichi.walk.WalkState
import com.wanderwildwood.yorimichi.walk.bearing
import com.wanderwildwood.yorimichi.walk.coordinate
import com.wanderwildwood.yorimichi.walk.distance
import com.wanderwildwood.yorimichi.walk.fixLine
import com.wanderwildwood.yorimichi.walk.radii
import com.wanderwildwood.yorimichi.walk.reading

/**
 * The whole app: a button, and then somewhere to walk.
 *
 * There is no map on this screen and there never will be. A greyscale raster basemap
 * that pans and zooms is the worst thing this panel can be asked to draw, and the phone
 * already has an offline map that does it properly — so the coordinate is handed over
 * to that one and this screen keeps the part that is actually useful while walking: an
 * arrow, a distance, and how far to trust it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkScreen(
    state: WalkState,
    onGo: () -> Unit,
    onAgain: () -> Unit,
    onOpen: (Coord) -> Unit,
    onSettings: () -> Unit,
    onAllow: () -> Unit,
    canHandOff: Boolean,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = "Detour", fontSize = 24.sp, fontWeight = FontWeight.Medium) },
                actions = {
                    Box(
                        modifier = Modifier.size(48.dp).clickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                !state.permitted -> Asking(onAllow)
                state.attractor == null -> Before(state, onGo)
                else -> After(state, onAgain, onOpen, canHandOff)
            }
        }
    }
}

@Composable
private fun ColumnScope.Asking(onAllow: () -> Unit) {
    Spacer(Modifier.weight(1f))
    TextMMD(
        text = "Detour needs to know where you are, to scatter its points around you.",
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    TextMMD(
        text = "It is not stored and it is not sent anywhere. The app has no way to " +
            "reach the network.",
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    ButtonMMD(
        onClick = onAllow,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { TextMMD(text = "Allow", fontSize = 18.sp, fontWeight = FontWeight.Medium) }
    Spacer(Modifier.weight(1.4f))
}

@Composable
private fun ColumnScope.Before(state: WalkState, onGo: () -> Unit) {
    Spacer(Modifier.weight(1f))

    val walk = radii(state.units).firstOrNull { it.metres == state.radiusMetres }
    TextMMD(
        text = "Within ${walk?.label ?: "${state.radiusMetres} m"}, ${where(state.look)}.",
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(10.dp))
    TextMMD(
        text = fixLine(state.fix, state.units),
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(28.dp))
    // No button at all until there is somewhere to start from. A button that is drawn
    // and then refuses is a button you press twice.
    if (state.fix != null) {
        ButtonMMD(
            onClick = onGo,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { TextMMD(text = "Go", fontSize = 22.sp, fontWeight = FontWeight.Medium) }
    }

    Spacer(Modifier.weight(1.4f))
}

@Composable
private fun ColumnScope.After(
    state: WalkState,
    onAgain: () -> Unit,
    onOpen: (Coord) -> Unit,
    canHandOff: Boolean,
) {
    val attractor = state.attractor ?: return

    Spacer(Modifier.height(8.dp))
    Arrow(
        bearingDegrees = attractor.bearingDegrees,
        headingDegrees = state.heading,
        modifier = Modifier.size(210.dp),
    )

    Spacer(Modifier.height(14.dp))
    TextMMD(
        text = distance(attractor.distanceMetres, state.units),
        fontSize = 44.sp,
        fontWeight = FontWeight.Medium,
    )
    TextMMD(
        text = if (state.heading == null) {
            // Said out loud, because with no compass the ring is drawn north-up and the
            // reader has to do the turning themselves.
            "${bearing(attractor.bearingDegrees)} — no compass on this phone"
        } else {
            bearing(attractor.bearingDegrees)
        },
        fontSize = 15.sp,
    )

    Spacer(Modifier.height(16.dp))
    TextMMD(text = coordinate(attractor.point), fontSize = 15.sp)

    Spacer(Modifier.height(10.dp))
    TextMMD(
        text = reading(attractor.concentration),
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.weight(1f))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Only offered when something on the phone actually answers a map. With no map
        // installed the coordinate above is the whole of the handoff, and a button that
        // opens nothing would be worse than no button.
        if (canHandOff) {
            OutlinedButtonMMD(
                onClick = { onOpen(attractor.point) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { TextMMD(text = "Open in maps", fontSize = 16.sp) }
        }

        OutlinedButtonMMD(
            onClick = onAgain,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = "Somewhere else", fontSize = 16.sp) }
    }
    Spacer(Modifier.height(4.dp))
}

/** How the two ends of the scatter are said in a sentence. */
fun where(look: Look): String = when (look) {
    Look.GATHER -> "where the points gather"
    Look.THIN -> "where they thin out"
}
