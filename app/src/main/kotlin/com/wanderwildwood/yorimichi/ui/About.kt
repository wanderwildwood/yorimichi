package com.wanderwildwood.yorimichi.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.yorimichi.BuildConfig
import com.wanderwildwood.yorimichi.R

private const val MAIN = 0
private const val PRIVACY = 1
private const val TERMS = 2
private const val LICENCES = 3

/**
 * What this is, the safety notice, and rows for Privacy, Terms and Licences, each a page of its
 * own. The list pages by whole rows like every list here; nothing scrolls.
 *
 * The line about the randomness is here because a stranger cannot safely assume the answer:
 * every other app of this kind fetches its numbers from a server, and two of them make a claim
 * about those numbers that this one does not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(MAIN) }
    BackHandler(enabled = page != MAIN) { page = MAIN }
    if (page == LICENCES) {
        LicencesScreen(onBack = { page = MAIN })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = {
                    TextMMD(
                        text = when (page) {
                            PRIVACY -> stringResource(R.string.about_privacy_heading)
                            TERMS -> stringResource(R.string.about_terms_heading)
                            else -> stringResource(R.string.about_title, BuildConfig.VERSION_NAME)
                        },
                    )
                },
                navigationIcon = {
                    BarButton(
                        Icons.Close,
                        stringResource(R.string.settings_cd_close),
                        { if (page == MAIN) onBack() else page = MAIN },
                    )
                },
            )
        },
    ) { contentPadding ->
        PagedColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (page) {
                PRIVACY -> item { Body(stringResource(R.string.about_privacy)) }
                TERMS -> item { Body(stringResource(R.string.about_terms)) }
                else -> {
                    item { Body(stringResource(R.string.about_what)) }
                    item { Body(stringResource(R.string.about_randomness)) }
                    item { Heading(stringResource(R.string.about_safety_heading)) }
                    item { Body(stringResource(R.string.safety_notice)) }
                    item { NavRow(stringResource(R.string.about_privacy_heading)) { page = PRIVACY } }
                    item { NavRow(stringResource(R.string.about_terms_heading)) { page = TERMS } }
                    item { NavRow(stringResource(R.string.licences_title)) { page = LICENCES } }
                    item { Llama() }
                }
            }
        }
    }
}

/** A row that opens a page of its own: 48 dp tall to press, with an arrow at the end. */
@Composable
private fun NavRow(text: String, onPress: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onPress),
    ) {
        TextMMD(text = text, style = MaterialTheme.typography.bodyLarge)
        TextMMD(text = "\u203A", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Heading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun Body(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodySmall)
}

private fun open(context: android.content.Context, address: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address)))
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.about_no_browser), Toast.LENGTH_SHORT).show()
    }
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to. The site's
 * address sits at the start of the same line and opens the site; the llama and its words open
 * the page. The drawing is ink rather than an emoji, which reaches the panel as a pale smudge.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextMMD(
            text = "wanderthe.dev",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable { open(context, "https://wanderthe.dev") }
                .padding(vertical = 12.dp),
        )
        Spacer(Modifier.width(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable { open(context, "https://square.link/u/AGu8oT10") },
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_the_llamas), style = MaterialTheme.typography.bodySmall)
        }
    }
}
