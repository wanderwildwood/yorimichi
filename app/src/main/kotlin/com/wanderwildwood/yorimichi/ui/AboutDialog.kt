package com.wanderwildwood.yorimichi.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.yorimichi.BuildConfig

/**
 * What this is, what it does with what it knows, and where the source lives.
 *
 * The line about the randomness is here because a stranger cannot safely assume the
 * answer: every other app of this kind fetches its numbers from a server, and two of
 * them make a claim about those numbers that this one does not.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        TextMMD(
            text = "Detour ${BuildConfig.VERSION_NAME}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = "Your position never leaves the phone, and the app cannot reach the " +
                "network at all.",
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(14.dp))
        TextMMD(
            text = "The numbers come from the phone's own generator. They are not " +
                "quantum, and nothing here has been shown to know anything.",
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(14.dp))
        TextMMD(text = "GNU General Public License v3", fontSize = 14.sp)
        TextMMD(text = "Icons from Material Symbols, Apache 2.0", fontSize = 14.sp)

        Spacer(Modifier.height(14.dp))
        TextMMD(text = "github.com/wanderwildwood/yorimichi", fontSize = 14.sp)

        Spacer(Modifier.height(18.dp))
        OutlinedButtonMMD(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { TextMMD(text = "Close", fontSize = 15.sp) }
    }
}
