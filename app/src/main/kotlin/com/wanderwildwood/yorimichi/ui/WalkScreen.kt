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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.yorimichi.R
import com.wanderwildwood.yorimichi.core.Coord
import com.wanderwildwood.yorimichi.walk.ARRIVED_METRES
import com.wanderwildwood.yorimichi.walk.FixLine
import com.wanderwildwood.yorimichi.walk.Leg
import com.wanderwildwood.yorimichi.walk.age
import com.wanderwildwood.yorimichi.walk.Look
import com.wanderwildwood.yorimichi.walk.RadiusLabel
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
    onAppSettings: () -> Unit,
    onLocationSettings: () -> Unit,
    canHandOff: Boolean,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.walk_title)) },
                actions = {
                    Box(
                        modifier = Modifier.size(48.dp).clickable(onClick = onSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Settings,
                            contentDescription = stringResource(R.string.walk_cd_settings),
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
                !state.permitted && state.refusedForGood -> Refused(onAppSettings)
                !state.permitted -> Asking(onAllow)
                state.attractor == null -> Before(state, onGo, onLocationSettings)
                else -> After(state, onAgain, onOpen, canHandOff)
            }
        }
    }
}

@Composable
private fun ColumnScope.Asking(onAllow: () -> Unit) {
    Spacer(Modifier.weight(1f))
    TextMMD(
        text = stringResource(R.string.walk_asking_why),
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    TextMMD(
        text = stringResource(R.string.walk_asking_privacy),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    ButtonMMD(
        onClick = onAllow,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { TextMMD(text = stringResource(R.string.walk_allow), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
    Spacer(Modifier.weight(1.4f))
}

@Composable
private fun ColumnScope.Refused(onAppSettings: () -> Unit) {
    Spacer(Modifier.weight(1f))
    TextMMD(
        text = stringResource(R.string.walk_refused_why),
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    TextMMD(
        text = stringResource(R.string.walk_refused_how),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(28.dp))
    ButtonMMD(
        onClick = onAppSettings,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { TextMMD(text = stringResource(R.string.walk_open_app_settings), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
    Spacer(Modifier.weight(1.4f))
}

@Composable
private fun ColumnScope.Before(state: WalkState, onGo: () -> Unit, onLocationSettings: () -> Unit) {
    Spacer(Modifier.weight(1f))

    val walk = radii(state.units).firstOrNull { it.metres == state.radiusMetres }
    TextMMD(
        text = stringResource(R.string.walk_within, said(walk?.label ?: RadiusLabel.Metres(state.radiusMetres)), where(state.look)),
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
    )

    val line = fixLine(
        fix = state.fix,
        units = state.units,
        now = state.now,
        locationOn = state.locationOn,
        searchedMillis = state.searchingSince?.let { state.now - it } ?: 0,
    )
    Spacer(Modifier.height(10.dp))
    TextMMD(
        text = said(line),
        style = MaterialTheme.typography.labelSmall,
        // Bold when there is no fresh position, because then this line is the news.
        fontWeight = if (line is FixLine.GoodTo || line is FixLine.UnknownAccuracy) null else FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    // The why, and what to do about it, wherever the phone is not simply in the open.
    val hint = when {
        line is FixLine.Stale && state.locationOn -> stringResource(R.string.hint_stale_looking)
        line is FixLine.Stale -> stringResource(R.string.hint_stale_off)
        line == FixLine.Searching -> stringResource(R.string.hint_searching)
        line == FixLine.NotFound -> stringResource(R.string.hint_not_found)
        line == FixLine.Off -> stringResource(R.string.hint_off)
        else -> null
    }
    if (hint != null) {
        Spacer(Modifier.height(6.dp))
        TextMMD(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }

    Spacer(Modifier.height(28.dp))
    // No button at all until there is somewhere to start from. A button that is drawn
    // and then refuses is a button you press twice.
    if (state.fix != null) {
        ButtonMMD(
            onClick = onGo,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) { TextMMD(text = stringResource(R.string.walk_go), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) }
    }
    if (!state.locationOn) {
        Spacer(Modifier.height(12.dp))
        OutlinedButtonMMD(
            onClick = onLocationSettings,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = stringResource(R.string.walk_turn_on_location), style = MaterialTheme.typography.titleSmall) }
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
    // From the phone's position now, on every fix; the numbers the walk started with are
    // only the fallback for a screen that somehow has no position at all.
    val leg = state.leg ?: Leg(attractor.distanceMetres, attractor.bearingDegrees, false)

    Spacer(Modifier.height(8.dp))
    if (leg.arrived) {
        Spacer(Modifier.height(60.dp))
        TextMMD(
            text = stringResource(R.string.walk_arrived),
            fontSize = 44.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        TextMMD(
            text = stringResource(R.string.walk_arrived_within, said(distance(ARRIVED_METRES, state.units))),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    } else {
        Arrow(
            bearingDegrees = leg.bearingDegrees,
            headingDegrees = state.heading,
            modifier = Modifier.size(210.dp),
        )

        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = said(distance(leg.distanceMetres, state.units)),
            fontSize = 44.sp,
            fontWeight = FontWeight.Medium,
        )
        TextMMD(
            text = if (state.heading == null) {
                // Said out loud, because with no compass the ring is drawn north-up and the
                // reader has to do the turning themselves.
                stringResource(R.string.walk_bearing_no_compass, said(bearing(leg.bearingDegrees)))
            } else {
                said(bearing(leg.bearingDegrees))
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (state.positionLost) {
        Spacer(Modifier.height(8.dp))
        val fix = state.fix
        TextMMD(
            text = when {
                !state.locationOn -> stringResource(R.string.walk_lost_off)
                fix == null -> stringResource(R.string.walk_lost_none)
                else -> stringResource(R.string.walk_lost_old, said(age(fix.ageMillis(state.now))))
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }

    Spacer(Modifier.height(16.dp))
    TextMMD(text = coordinate(attractor.point), style = MaterialTheme.typography.bodySmall)

    Spacer(Modifier.height(10.dp))
    TextMMD(
        text = said(reading(attractor.concentration)),
        style = MaterialTheme.typography.labelSmall,
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
            ) { TextMMD(text = stringResource(R.string.walk_open_in_maps), style = MaterialTheme.typography.titleSmall) }
        }

        OutlinedButtonMMD(
            onClick = onAgain,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = stringResource(R.string.walk_somewhere_else), style = MaterialTheme.typography.titleSmall) }
    }
    Spacer(Modifier.height(4.dp))
}

/** How the two ends of the scatter are said in a sentence. */
@Composable
fun where(look: Look): String = when (look) {
    Look.GATHER -> stringResource(R.string.look_gather)
    Look.THIN -> stringResource(R.string.look_thin)
}
