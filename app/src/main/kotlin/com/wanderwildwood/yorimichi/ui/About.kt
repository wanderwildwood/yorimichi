package com.wanderwildwood.yorimichi.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.yorimichi.BuildConfig
import com.wanderwildwood.yorimichi.R

private const val PAGES = 3

/**
 * What this is, what it does with what it knows, the terms, whose work it stands on and where
 * the source is. Three pages with Back and Next at the foot rather than one that scrolls: the
 * words are too many for a dialog and nothing here scrolls.
 *
 * The line about the randomness is here because a stranger cannot safely assume the answer:
 * every other app of this kind fetches its numbers from a server, and two of them make a claim
 * about those numbers that this one does not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.about_title, BuildConfig.VERSION_NAME)) },
                navigationIcon = { BarButton(Icons.Close, stringResource(R.string.settings_cd_close), onBack) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp),
        ) {
            Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                when (page) {
                    0 -> {
                        Body(stringResource(R.string.about_what))
                        Spacer(Modifier.height(14.dp))
                        Body(stringResource(R.string.about_randomness))
                        Spacer(Modifier.height(18.dp))
                        Heading(stringResource(R.string.about_safety_heading))
                        Body(stringResource(R.string.safety_notice))
                    }

                    1 -> {
                        Heading(stringResource(R.string.about_privacy_heading))
                        Body(stringResource(R.string.about_privacy))
                    }

                    else -> {
                        Heading(stringResource(R.string.about_terms_heading))
                        Body(stringResource(R.string.about_terms))
                        Spacer(Modifier.height(18.dp))
                        Heading(stringResource(R.string.about_made_heading))
                        Body(stringResource(R.string.about_licence))
                        Body(stringResource(R.string.about_libraries))
                        Body(stringResource(R.string.about_icons))
                        Link(stringResource(R.string.about_source_text)) {
                            open(context, "https://github.com/wanderwildwood/yorimichi")
                        }
                        Llama()
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (page > 0) {
                    OutlinedButtonMMD(
                        onClick = { page -= 1 },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { TextMMD(text = stringResource(R.string.about_page_back), style = MaterialTheme.typography.titleSmall) }
                }
                if (page < PAGES - 1) {
                    OutlinedButtonMMD(
                        onClick = { page += 1 },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { TextMMD(text = stringResource(R.string.about_page_next), style = MaterialTheme.typography.titleSmall) }
                }
            }
        }
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

/** A line that opens an address: 48 dp tall to press. */
@Composable
private fun Link(text: String, onPress: () -> Unit) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onPress).padding(vertical = 12.dp),
    )
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
