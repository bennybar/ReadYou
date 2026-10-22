package me.ash.reader.ui.page.settings.whatsnew

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import me.ash.reader.R
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.theme.palette.onLight

/** One release's highlights, as shown in What's new. */
class Release(val version: String, val date: String, val highlights: List<String>)

/**
 * What's new, newest first. Add the new release at the top with every release; the version must
 * match versionName, which is how the sheet knows what is new since the last one seen.
 */
val CHANGELOG =
    listOf(
        Release(
            "0.26.4",
            "Oct 22, 2026",
            listOf("Swiping an article reveals its action as a rounded block, a little apart from the card"),
        ),
        Release(
            "0.26.3",
            "Oct 22, 2026",
            listOf("Smoother swipes: one color from the start, and the card keeps its rounded corners as it slides"),
        ),
        Release(
            "0.26.2",
            "Oct 22, 2026",
            listOf(
                "Articles always open at the top. To come back where you stopped, turn on Settings → Offline → Remember where I stopped reading",
                "Swiping an article shows its color in the card's rounded shape",
            ),
        ),
        Release(
            "0.26.1",
            "Oct 22, 2026",
            listOf(
                "A more refined bottom toolbar: a fine outline and soft shadow, even buttons, and a softer selected filter",
                "A clearer Unread icon",
            ),
        ),
        Release(
            "0.26.0",
            "Oct 22, 2026",
            listOf(
                "A new Material 3 Expressive look, on by default (Settings → Color & style → Expressive style)",
                "Home: big filter cards with counts and a floating filter toolbar",
                "Articles as rounded cards with an Offline or Text only chip",
                "Reader: a floating toolbar with a big Next button",
                "Offline page: see how much of the archive is used, and download or stop from one card",
                "What's new: this list, once after each update, and any time in Settings",
            ),
        ),
        Release(
            "0.25.0",
            "Oct 22, 2026",
            listOf(
                "Every article is searchable as soon as it arrives, and search understands Hebrew prefixes",
                "Search results highlight the words, and show the text around a match deep in an article",
                "Fixed articles squeezed into a narrow column, and headlines shown twice",
                "The fresh copy downloaded on opening an article keeps your place",
                "New Settings → Offline page; the image archive is sized in GB",
                "Star, Read Later and read changes made offline are sent on the next sync",
                "A pin in the article list shows what is saved offline",
            ),
        ),
        Release(
            "0.24.0",
            "Oct 22, 2026",
            listOf(
                "Opening an article re-downloads it behind the copy you are reading",
                "Hebrew and Arabic take their direction from most of the text, in lists and the reader",
                "The reader remembers where you stopped",
                "Offline downloads finish each article before the next; Clear offline archive",
            ),
        ),
        Release(
            "0.16 – 0.23",
            "Jul 2026",
            listOf(
                "Full content and images for every feed, kept offline",
                "Full-text search, Read Later synced to FreshRSS, duplicate stories collapsed",
                "Sync log, unread count on the app icon",
            ),
        ),
    )

private const val PREFS = "whats_new"
private const val SEEN = "seen"

private fun parts(v: String) = v.substringBefore(' ').split('.').map { it.toIntOrNull() ?: 0 }

/** Whether release [a] (its first version, for a range) is newer than version [b]. */
internal fun newer(a: String, b: String): Boolean {
    val x = parts(a)
    val y = parts(b)
    for (i in 0 until maxOf(x.size, y.size)) {
        val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
        if (d != 0) return d > 0
    }
    return false
}

/**
 * The releases to show once after an update: those since the version last seen here, or this
 * release's alone the first time. Nothing on a fresh install, where nothing is "new". Marks the
 * current version seen.
 */
internal fun releasesToShow(context: Context): List<Release> {
    val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: return emptyList()
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val seen = prefs.getString(SEEN, null)
    prefs.edit { putString(SEEN, current) }
    if (seen == current) return emptyList()
    if (seen == null) {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (info.firstInstallTime == info.lastUpdateTime) return emptyList()
        return CHANGELOG.take(1)
    }
    return CHANGELOG.filter { newer(it.version, seen) }.take(3)
}

/** Shown on the home screen: once after an update, a sheet of what changed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(onShowAll: () -> Unit) {
    val context = LocalContext.current
    var releases by remember { mutableStateOf(emptyList<Release>()) }
    LaunchedEffect(Unit) { releases = releasesToShow(context) }
    if (releases.isEmpty()) return

    ModalBottomSheet(onDismissRequest = { releases = emptyList() }) {
        Column(
            Modifier.navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.whats_new_in, releases.first().version),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            releases.forEach { ReleaseNotes(it, showVersion = releases.size > 1) }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        releases = emptyList()
                        onShowAll()
                    }
                ) {
                    Text(stringResource(R.string.whats_new_all))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { releases = emptyList() }) { Text(stringResource(R.string.got_it)) }
            }
        }
    }
}

@Composable
private fun ReleaseNotes(release: Release, showVersion: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(bottom = 12.dp)) {
        if (showVersion) {
            Row {
                Text(release.version, fontWeight = FontWeight.Bold, color = colors.primary)
                Spacer(Modifier.width(8.dp))
                Text(release.date, fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
        }
        release.highlights.forEach {
            Row(Modifier.padding(bottom = 6.dp)) {
                Text("•", color = colors.primary)
                Spacer(Modifier.width(10.dp))
                Text(it)
            }
        }
    }
}

/** Settings → What's new: every release's highlights. */
@Composable
fun WhatsNewPage(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    RYScaffold(
        containerColor = colors.surface onLight colors.inverseOnSurface,
        navigationIcon = {
            FeedbackIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = colors.onSurface,
                onClick = onBack,
            )
        },
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { DisplayText(text = stringResource(R.string.whats_new), desc = "") }
                items(CHANGELOG, key = { it.version }) { release ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = colors.surfaceContainerLow,
                    ) {
                        Column(Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 6.dp)) {
                            ReleaseNotes(release)
                        }
                    }
                }
                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
            }
        },
    )
}
