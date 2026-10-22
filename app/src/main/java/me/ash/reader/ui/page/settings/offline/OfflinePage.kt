package me.ash.reader.ui.page.settings.offline

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
    val fullContentAllFeeds = LocalFullContentAllFeeds.current
    val prefetchImages = LocalPrefetchImages.current
    val prefetchScope = LocalPrefetchScope.current
    val imageCacheSize = LocalImageCacheSize.current
    val refetchOnOpen = LocalRefetchOnOpen.current
    val collapseDuplicates = LocalCollapseDuplicates.current
    val markDuplicatesRead = LocalMarkDuplicatesRead.current
    val readHistory = LocalReadHistory.current

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
                    SettingItem(
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
