package me.ash.reader.ui.page.settings.offline

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import me.ash.reader.infrastructure.preference.LocalRememberReadingPosition
import me.ash.reader.infrastructure.preference.LocalExpressiveStyle
import me.ash.reader.domain.data.PrefetchSummary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.ash.reader.R
import me.ash.reader.infrastructure.preference.ImageCacheSizePreference
import me.ash.reader.infrastructure.preference.LocalCollapseDuplicates
import me.ash.reader.infrastructure.preference.LocalFullContentAllFeeds
import me.ash.reader.infrastructure.preference.LocalImageCacheSize
import me.ash.reader.infrastructure.preference.LocalMarkDuplicatesRead
import me.ash.reader.infrastructure.preference.LocalPrefetchImages
import me.ash.reader.infrastructure.preference.LocalPrefetchScope
import me.ash.reader.infrastructure.preference.LocalReadHistory
import me.ash.reader.infrastructure.preference.LocalRefetchOnOpen
import me.ash.reader.infrastructure.preference.PrefetchScopePreference
import me.ash.reader.infrastructure.preference.ReadHistoryPreference
import me.ash.reader.infrastructure.preference.RefetchOnOpenPreference
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYDialog
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.component.base.RYSwitch
import me.ash.reader.ui.component.base.RadioDialog
import me.ash.reader.ui.component.base.RadioDialogOption
import me.ash.reader.ui.component.base.Subtitle
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.ext.showToast
import me.ash.reader.ui.page.settings.SettingItem
import me.ash.reader.ui.page.settings.interaction.PrefetchViewModel
import me.ash.reader.ui.theme.palette.onLight

/** Everything about keeping articles on the device: downloads, images, freshness, duplicates. */
@Composable
fun OfflinePage(onBack: () -> Unit, prefetchViewModel: PrefetchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefetchProgress = prefetchViewModel.progress.collectAsStateValue()
    val lastPrefetchRun = prefetchViewModel.lastRun.collectAsStateValue()
    val usage = prefetchViewModel.usage.collectAsStateValue()
    val expressive = LocalExpressiveStyle.current.value
    // A run that just ended changed what is on disk.
    LaunchedEffect(prefetchProgress == null) { prefetchViewModel.refreshUsage() }
    val fullContentAllFeeds = LocalFullContentAllFeeds.current
    val prefetchImages = LocalPrefetchImages.current
    val prefetchScope = LocalPrefetchScope.current
    val imageCacheSize = LocalImageCacheSize.current
    val refetchOnOpen = LocalRefetchOnOpen.current
    val collapseDuplicates = LocalCollapseDuplicates.current
    val markDuplicatesRead = LocalMarkDuplicatesRead.current
    val readHistory = LocalReadHistory.current
    val rememberReadingPosition = LocalRememberReadingPosition.current

    var clearArchiveDialogVisible by remember { mutableStateOf(false) }
    var prefetchScopeDialogVisible by remember { mutableStateOf(false) }
    var imageCacheSizeDialogVisible by remember { mutableStateOf(false) }
    var refetchOnOpenDialogVisible by remember { mutableStateOf(false) }
    var readHistoryDialogVisible by remember { mutableStateOf(false) }

    RYScaffold(
        containerColor =
            MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface,
        navigationIcon = {
            FeedbackIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onBack,
            )
        },
        content = {
            LazyColumn {
                item {
                    DisplayText(text = stringResource(R.string.offline), desc = "")
                    if (expressive) {
                        ArchiveCard(
                            usage = usage,
                            progress = prefetchProgress,
                            lastRun = lastPrefetchRun,
                            onDownload = {
                                if (prefetchProgress != null) prefetchViewModel.stop()
                                else prefetchViewModel.downloadNow()
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.parse_full_content),
                    )
                    SettingItem(
                        title = stringResource(R.string.full_content_all_feeds),
                        desc = stringResource(R.string.full_content_all_feeds_desc),
                        onClick = { fullContentAllFeeds.toggle(context, scope) },
                    ) {
                        RYSwitch(activated = fullContentAllFeeds.value) {
                            fullContentAllFeeds.toggle(context, scope)
                        }
                    }
                    SettingItem(
                        title = stringResource(R.string.prefetch_images),
                        desc = stringResource(R.string.prefetch_images_desc),
                        onClick = { prefetchImages.toggle(context, scope) },
                    ) {
                        RYSwitch(activated = prefetchImages.value) {
                            prefetchImages.toggle(context, scope)
                        }
                    }
                    SettingItem(
                        title = stringResource(R.string.prefetch_scope),
                        desc = prefetchScope.toDesc(context),
                        onClick = { prefetchScopeDialogVisible = true },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.image_cache_size),
                        desc = imageCacheSize.toDesc(context),
                        onClick = { imageCacheSizeDialogVisible = true },
                    ) {}
                    if (!expressive) SettingItem(
                        title = stringResource(R.string.download_now),
                        desc =
                            prefetchProgress?.let {
                                stringResource(R.string.download_progress_stop, it.current, it.total)
                            }
                                ?: lastPrefetchRun?.let {
                                    if (it.interrupted)
                                        stringResource(
                                            R.string.download_interrupted,
                                            it.current,
                                            it.total,
                                        )
                                    else
                                        stringResource(
                                            R.string.download_summary,
                                            it.ready,
                                            it.textOnly,
                                            it.failed,
                                        )
                                }
                                ?: stringResource(R.string.download_now_desc),
                        // While a run is going, the same row stops it rather than restarting it.
                        onClick = {
                            if (prefetchProgress != null) prefetchViewModel.stop()
                            else prefetchViewModel.downloadNow()
                        },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.clear_archive),
                        desc = stringResource(R.string.clear_archive_desc),
                        onClick = { clearArchiveDialogVisible = true },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.reading_page),
                    )
                    SettingItem(
                        title = stringResource(R.string.refetch_on_open),
                        desc = refetchOnOpen.toDesc(context),
                        onClick = { refetchOnOpenDialogVisible = true },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.remember_reading_position),
                        desc = stringResource(R.string.remember_reading_position_desc),
                        onClick = { rememberReadingPosition.toggle(context, scope) },
                    ) {
                        RYSwitch(activated = rememberReadingPosition.value) {
                            rememberReadingPosition.toggle(context, scope)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.offline_sync_section),
                    )
                    SettingItem(
                        title = stringResource(R.string.collapse_duplicates),
                        desc = stringResource(R.string.collapse_duplicates_desc),
                        onClick = { collapseDuplicates.toggle(context, scope) },
                    ) {
                        RYSwitch(activated = collapseDuplicates.value) {
                            collapseDuplicates.toggle(context, scope)
                        }
                    }
                    SettingItem(
                        title = stringResource(R.string.mark_duplicates_read),
                        desc = stringResource(R.string.mark_duplicates_read_desc),
                        enabled = collapseDuplicates.value,
                        onClick = { markDuplicatesRead.toggle(context, scope) },
                    ) {
                        RYSwitch(
                            activated = markDuplicatesRead.value,
                            enable = collapseDuplicates.value,
                        ) {
                            markDuplicatesRead.toggle(context, scope)
                        }
                    }
                    SettingItem(
                        title = stringResource(R.string.read_history),
                        desc = readHistory.toDesc(context),
                        onClick = { readHistoryDialogVisible = true },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    )

    RYDialog(
        visible = clearArchiveDialogVisible,
        onDismissRequest = { clearArchiveDialogVisible = false },
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = stringResource(R.string.clear_archive),
            )
        },
        title = { Text(text = stringResource(R.string.clear_archive)) },
        text = { Text(text = stringResource(R.string.clear_archive_tips)) },
        confirmButton = {
            TextButton(
                onClick = {
                    prefetchViewModel.clearArchive {
                        clearArchiveDialogVisible = false
                        context.showToast(context.getString(R.string.clear_archive_toast))
                    }
                }
            ) {
                Text(text = stringResource(R.string.clear))
            }
        },
        dismissButton = {
            TextButton(onClick = { clearArchiveDialogVisible = false }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )

    RadioDialog(
        visible = prefetchScopeDialogVisible,
        title = stringResource(R.string.prefetch_scope),
        options =
            PrefetchScopePreference.values.map {
                RadioDialogOption(text = it.toDesc(context), selected = it == prefetchScope) {
                    it.put(context, scope)
                }
            },
    ) {
        prefetchScopeDialogVisible = false
    }

    RadioDialog(
        visible = imageCacheSizeDialogVisible,
        title = stringResource(R.string.image_cache_size),
        options =
            ImageCacheSizePreference.values.map {
                RadioDialogOption(text = it.toDesc(context), selected = it == imageCacheSize) {
                    it.put(context, scope)
                }
            },
    ) {
        imageCacheSizeDialogVisible = false
    }

    RadioDialog(
        visible = refetchOnOpenDialogVisible,
        title = stringResource(R.string.refetch_on_open),
        options =
            RefetchOnOpenPreference.values.map {
                RadioDialogOption(text = it.toDesc(context), selected = it == refetchOnOpen) {
                    it.put(context, scope)
                }
            },
    ) {
        refetchOnOpenDialogVisible = false
    }

    RadioDialog(
        visible = readHistoryDialogVisible,
        title = stringResource(R.string.read_history),
        options =
            ReadHistoryPreference.values.map {
                RadioDialogOption(text = it.toDesc(context), selected = it == readHistory) {
                    it.put(context, scope)
                }
            },
    ) {
        readHistoryDialogVisible = false
    }
}

/**
 * The expressive style's archive summary: how much of the image archive is used, how the last
 * download went (or how far the current one is), and Download now / Stop.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArchiveCard(
    usage: PrefetchViewModel.ArchiveUsage?,
    progress: PrefetchSummary?,
    lastRun: PrefetchSummary?,
    onDownload: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(32.dp),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.offline_archive),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (usage != null && usage.imageMaxBytes > 0) {
                    Text(
                        text =
                            stringResource(
                                R.string.archive_used_of,
                                binarySize(usage.imageBytes),
                                binarySize(usage.imageMaxBytes),
                            ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            if (usage != null && usage.imageMaxBytes > 0) {
                LinearProgressIndicator(
                    progress = { (usage.imageBytes.toFloat() / usage.imageMaxBytes).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = colors.primary,
                    trackColor = colors.surface,
                    gapSize = 4.dp,
                    drawStopIndicator = {},
                )
                Text(
                    text =
                        stringResource(
                            R.string.archive_text_size,
                            binarySize(usage.textBytes),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (progress != null && progress.total > 0) {
                LinearWavyProgressIndicator(
                    progress = { progress.current.toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.surface,
                )
            }
            val status =
                progress?.let { stringResource(R.string.download_progress, it.current, it.total) }
                    ?: lastRun?.let {
                        if (it.interrupted)
                            stringResource(R.string.download_interrupted, it.current, it.total)
                        else stringResource(R.string.download_summary, it.ready, it.textOnly, it.failed)
                    }
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelLarge,
                    modifier =
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = colors.onSurface,
                )
            }
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    text =
                        stringResource(if (progress != null) R.string.stop else R.string.download_now),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Sizes in units of 1024, as the image cache size is set: Android's formatter counts in 1000s and
 * would show the 2 GB setting as "2.1 GB".
 */
private fun binarySize(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0 || value >= 10) "%.0f %s".format(value, units[unit])
    else "%.1f %s".format(value, units[unit]).replace(".0 ", " ")
}
