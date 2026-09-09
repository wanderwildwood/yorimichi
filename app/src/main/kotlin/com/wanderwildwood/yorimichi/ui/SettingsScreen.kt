package com.wanderwildwood.yorimichi.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.yorimichi.walk.Units
import com.wanderwildwood.yorimichi.walk.WalkState
import com.wanderwildwood.yorimichi.walk.radii

/**
 * The three things there are to set.
 *
 * Each row says what it is set to and nothing else, and a press moves it on to the next
 * value. Three settings do not want a screen each, and a picker is two full repaints to
 * choose between five things that fit on the row already.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: WalkState,
    onClose: () -> Unit,
    onRadius: () -> Unit,
    onLook: () -> Unit,
    onUnits: () -> Unit,
) {
    var aboutOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = "Settings", fontSize = 24.sp) },
                navigationIcon = {
                    BarButton(Icons.Close, "Close", onClose)
                },
                actions = {
                    BarButton(Icons.Info, "About", { aboutOpen = true })
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            val walk = radii(state.units).firstOrNull { it.metres == state.radiusMetres }
            Setting(
                title = "How far it may send you",
                value = walk?.label ?: "${state.radiusMetres} m",
                onClick = onRadius,
            )
            Setting(
                title = "Look for",
                value = where(state.look).replaceFirstChar { it.uppercase() },
                onClick = onLook,
            )
            Setting(
                title = "Distances",
                value = when (state.units) {
                    Units.IMPERIAL -> "Miles and feet"
                    Units.METRIC -> "Kilometres and metres"
                },
                onClick = onUnits,
            )
        }
    }

    if (aboutOpen) AboutDialog(onDismiss = { aboutOpen = false })
}

@Composable
private fun Setting(title: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        TextMMD(text = title, fontSize = 18.sp)
        TextMMD(text = value, fontSize = 14.sp)
    }
}

@Composable
private fun BarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}
