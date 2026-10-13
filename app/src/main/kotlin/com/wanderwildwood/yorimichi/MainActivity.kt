package com.wanderwildwood.yorimichi

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.yorimichi.device.canOpenMap
import com.wanderwildwood.yorimichi.device.openMap
import com.wanderwildwood.yorimichi.ui.SettingsScreen
import com.wanderwildwood.yorimichi.ui.WalkScreen
import com.wanderwildwood.yorimichi.ui.monochrome
import com.wanderwildwood.yorimichi.walk.WalkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD(colorScheme = monochrome) {
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

    val activity = context as ComponentActivity
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Asked straight after a refusal, Android only answers true here when it would
        // still show the dialog. False means it has stopped asking, and the system
        // settings are the only way left.
        val canAskAgain = activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        viewModel.permissionAnswered(granted, canAskAgain)
    }

    // Coming back from the system settings, where either switch may have been turned.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resumed() }

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
            onAppSettings = {
                openSettings(
                    context,
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
            onLocationSettings = { openSettings(context, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            canHandOff = canHandOff,
        )
    }
}

/** Open a page of the system settings, or the top of them where that page is missing. */
private fun openSettings(context: android.content.Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
}
