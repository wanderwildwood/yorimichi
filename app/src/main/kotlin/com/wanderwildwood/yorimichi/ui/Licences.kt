package com.wanderwildwood.yorimichi.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.yorimichi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One piece of other people's work that ships inside the app, and the licence it comes under. */
data class Library(
    val name: String,
    val by: String,
    val licence: String,
    val link: String,
)

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/yorimichi"

/**
 * Everything the APK carries that this project did not write, as the build pulls it in.
 * Licences are the ones each project declares in its own published metadata.
 */
val LIBRARIES: List<Library> =
    listOf(
        Library("Mudita Mindful Design (MMD)", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
        Library("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com"),
        Library("Jetpack Compose (UI, Foundation, Material 3), Activity, Lifecycle, Core", "Google", "Apache-2.0", "https://developer.android.com/jetpack"),
        Library("Kotlin standard library and coroutines", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
        Library("Material Symbols icons", "Google", "Apache-2.0", "https://fonts.google.com/icons"),
    )

/**
 * A licence text as the paragraphs a reader turns pages by. The texts are wrapped at about
 * seventy columns with a blank line between paragraphs; each paragraph is joined into one
 * row, and no word is dropped or repeated by it.
 */
fun licenceParagraphs(text: String): List<String> =
    text
        .replace("\r\n", "\n")
        .split(Regex("\n[ \t]*\n"))
        .map { block -> block.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }

/**
 * The Licences page: where the source is, the licence this app is under, what it is built with,
 * and the full text of each licence once. The texts are raw resources, paged a paragraph to a
 * row like every list here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val gpl by produceTexts(context, R.raw.gpl_3_0)
    val apache by produceTexts(context, R.raw.apache_2_0)
    val ofl by produceTexts(context, R.raw.ofl_1_1)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = { TextMMD(text = stringResource(R.string.licences_title)) },
                navigationIcon = { BarButton(Icons.Close, stringResource(R.string.settings_cd_close), onBack) },
            )
        },
    ) { padding ->
        PagedColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Heading(stringResource(R.string.licences_source_heading)) }
            item { Link(stringResource(R.string.licences_source, SOURCE_URL.removePrefix("https://")), SOURCE_URL) }
            item { Heading(stringResource(R.string.licences_licence_heading)) }
            item { Paragraph(stringResource(R.string.licences_licence)) }
            item { Heading(stringResource(R.string.licences_built_heading)) }
            LIBRARIES.forEach { library ->
                item {
                    Link("${library.name}, ${library.by}: ${library.licence}", library.link)
                }
            }
            item { Heading(stringResource(R.string.licences_gpl)) }
            gpl.forEach { item { Paragraph(it) } }
            item { Heading(stringResource(R.string.licences_apache)) }
            apache.forEach { item { Paragraph(it) } }
            item { Heading(stringResource(R.string.licences_ofl)) }
            ofl.forEach { item { Paragraph(it) } }
        }
    }
}

@Composable
private fun produceTexts(context: Context, @RawRes resource: Int): State<List<String>> =
    produceState(initialValue = emptyList<String>(), resource) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    licenceParagraphs(context.resources.openRawResource(resource).bufferedReader().use { it.readText() })
                }.getOrDefault(emptyList())
            }
    }

@Composable
private fun Heading(text: String) {
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Paragraph(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodySmall)
}

/** A line that opens an address in a browser; a phone with none says so rather than doing nothing. */
@Composable
private fun Link(text: String, address: String) {
    val context = LocalContext.current
    val none = stringResource(R.string.licences_no_browser)
    TextMMD(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        textDecoration = TextDecoration.Underline,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address))) }
                        .onFailure { Toast.makeText(context, none, Toast.LENGTH_SHORT).show() }
                }
                .wrapContentHeight(Alignment.CenterVertically),
    )
}
