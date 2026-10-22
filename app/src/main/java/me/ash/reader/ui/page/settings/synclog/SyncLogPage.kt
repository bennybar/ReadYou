package me.ash.reader.ui.page.settings.synclog

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import me.ash.reader.R
import me.ash.reader.domain.data.SyncRecord
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.component.base.Subtitle
import me.ash.reader.ui.ext.collectAsStateValue

@Composable
fun SyncLogPage(onBack: () -> Unit, viewModel: SyncLogViewModel = hiltViewModel()) {
    val records = viewModel.records.collectAsStateValue()

    RYScaffold(
        navigationIcon = {
            FeedbackIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
            ) {
                onBack()
            }
        },
        actions = {
            if (records.isNotEmpty()) {
                TextButton(onClick = { viewModel.clear() }) {
                    Text(text = stringResource(R.string.clear))
                }
            }
        },
        content = {
            LazyColumn {
                item {
                    DisplayText(text = stringResource(R.string.sync_log), desc = "")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (viewModel.isBatteryRestricted) {
                    item {
                        BatteryRestrictionBanner()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (records.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                            text = stringResource(R.string.sync_log_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                } else {
                    // Split rather than merely labelled: the question this page exists to answer is
                    // "is background syncing actually happening", and an interleaved list buries it.
                    val background = records.filter { !it.foreground }
                    val inApp = records.filter { it.foreground }

                    item {
                        Subtitle(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            text = stringResource(R.string.sync_log_background),
                        )
                    }
                    if (background.isEmpty()) {
                        item {
                            Text(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                text = stringResource(R.string.sync_log_no_background),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    } else {
                        items(background) { record -> SyncLogItem(record) }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Subtitle(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            text = stringResource(R.string.sync_log_in_app),
                        )
                    }
                    if (inApp.isEmpty()) {
                        item {
                            Text(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                text = stringResource(R.string.sync_log_no_in_app),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    } else {
                        items(inApp) { record -> SyncLogItem(record) }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    )
}

/**
 * The single most common reason background syncs never happen: the system is allowed to doze the
 * app, so it defers the periodic sync until the app is next opened.
 */
@Composable
private fun BatteryRestrictionBanner() {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.battery_restricted_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.battery_restricted_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    runCatching {
                            context.startActivity(
                                Intent(
                                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        "package:${context.packageName}".toUri(),
                                    )
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        .onFailure {
                            // Some OEMs do not honour the direct request; fall back to the app's
                            // own settings page, from which the user can still change it.
                            context.startActivity(
                                Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        "package:${context.packageName}".toUri(),
                                    )
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                }
            ) {
                Text(text = stringResource(R.string.battery_restricted_action))
            }
        }
    }
}

@Composable
private fun SyncLogItem(record: SyncRecord) {
    val context = LocalContext.current

    // Formatted with the device's default locale and time zone, so the timestamp reads as the
    // local time the sync actually ran.
    val timestamp =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(record.startedAt))

    val seconds = TimeUnit.MILLISECONDS.toSeconds(record.durationMs)
    val duration =
        if (seconds < 1) context.getString(R.string.sync_log_duration_ms, record.durationMs)
        else context.getString(R.string.sync_log_duration_s, seconds)

    val trigger =
        if (record.manual) stringResource(R.string.sync_log_manual)
        else stringResource(R.string.sync_log_scheduled)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(end = 16.dp),
            imageVector =
                if (record.succeeded) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint =
                if (record.succeeded) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
        )
        Column {
            Text(text = timestamp, style = MaterialTheme.typography.titleSmall)
            Text(
                text =
                    if (record.succeeded) {
                        context.resources.getQuantityString(
                            R.plurals.sync_log_new_articles,
                            record.newArticles,
                            record.newArticles,
                        ) + " · $duration · $trigger"
                    } else {
                        stringResource(R.string.sync_log_failed) + " · $duration · $trigger"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
