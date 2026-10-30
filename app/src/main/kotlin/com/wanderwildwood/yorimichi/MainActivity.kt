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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.yorimichi.ui.EInkDialog
import com.wanderwildwood.yorimichi.walk.Preferences
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
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { answers ->
        // Detour needs the precise one; "approximate" alone is not enough, and is answered like a refusal.
        val granted = answers[Manifest.permission.ACCESS_FINE_LOCATION] == true
        // Asked straight after a refusal, Android only answers true here when it would
        // still show the dialog. False means it has stopped asking, and the system
        // settings are the only way left.
        val canAskAgain = activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        viewModel.permissionAnswered(granted, canAskAgain)
    }

    // Coming back from the system settings, where either switch may have been turned.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.resumed() }

    val preferences = remember(context) { Preferences(context) }
    var safetyOpen by remember { mutableStateOf(!preferences.safetySeen) }
    if (safetyOpen) {
        EInkDialog(onDismiss = {}) {
            TextMMD(text = stringResource(R.string.safety_notice), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            OutlinedButtonMMD(
                onClick = { preferences.safetySeen = true; safetyOpen = false },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { TextMMD(text = stringResource(R.string.safety_ok), style = MaterialTheme.typography.titleSmall) }
        }
    }

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
            onOpen = {
                if (!openMap(context, it)) {
                    android.widget.Toast.makeText(context, context.getString(R.string.walk_no_map), android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onSettings = { settingsOpen = true },
            onAllow = { ask.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
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
