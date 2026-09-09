package com.wanderwildwood.yorimichi

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.yorimichi.device.canOpenMap
import com.wanderwildwood.yorimichi.device.openMap
import com.wanderwildwood.yorimichi.ui.SettingsScreen
import com.wanderwildwood.yorimichi.ui.WalkScreen
import com.wanderwildwood.yorimichi.walk.WalkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD {
                Detour()
            }
        }
    }
}

@Composable
private fun Detour(viewModel: WalkViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var settingsOpen by remember { mutableStateOf(false) }
    // Asked once. Installing a map app while this screen is open is not a case worth
    // watching for.
    val canHandOff = remember(context) { canOpenMap(context) }

    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::permissionAnswered,
    )

    if (settingsOpen) {
        SettingsScreen(
            state = state,
            onClose = { settingsOpen = false },
            onRadius = viewModel::nextRadius,
            onLook = viewModel::toggleLook,
            onUnits = viewModel::toggleUnits,
        )
    } else {
        WalkScreen(
            state = state,
            onGo = viewModel::go,
            onAgain = viewModel::clear,
            onOpen = { openMap(context, it) },
            onSettings = { settingsOpen = true },
            onAllow = { ask.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            canHandOff = canHandOff,
        )
    }
}
