package me.ash.reader.ui.page.settings.interaction

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
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
import me.ash.reader.R
import me.ash.reader.infrastructure.preference.InitialFilterPreference
import me.ash.reader.infrastructure.preference.InitialPagePreference
import me.ash.reader.infrastructure.preference.LocalArticleListSwipeEndAction
import me.ash.reader.infrastructure.preference.LocalArticleListSwipeStartAction
import me.ash.reader.infrastructure.preference.ImageCacheSizePreference
import me.ash.reader.infrastructure.preference.LocalFullContentAllFeeds
import me.ash.reader.infrastructure.preference.LocalHideEmptyGroups
import me.ash.reader.infrastructure.preference.LocalImageCacheSize
import me.ash.reader.infrastructure.preference.LocalPrefetchScope
import me.ash.reader.infrastructure.preference.PrefetchScopePreference
import me.ash.reader.infrastructure.preference.LocalInitialFilter
import me.ash.reader.infrastructure.preference.LocalInitialPage
import me.ash.reader.infrastructure.preference.LocalMarkAsReadOnScroll
import me.ash.reader.infrastructure.preference.LocalOpenLink
import me.ash.reader.infrastructure.preference.LocalOpenLinkSpecificBrowser
import me.ash.reader.infrastructure.preference.LocalPrefetchImages
import me.ash.reader.infrastructure.preference.LocalPullToSwitchArticle
import me.ash.reader.infrastructure.preference.LocalSettings
import me.ash.reader.infrastructure.preference.LocalSharedContent
import me.ash.reader.infrastructure.preference.LocalSortUnreadArticles
import me.ash.reader.infrastructure.preference.OpenLinkPreference
import me.ash.reader.infrastructure.preference.PullToLoadNextFeedPreference
import me.ash.reader.infrastructure.preference.SharedContentPreference
import me.ash.reader.infrastructure.preference.SortUnreadArticlesPreference
import me.ash.reader.infrastructure.preference.SwipeEndActionPreference
import me.ash.reader.infrastructure.preference.SwipeStartActionPreference
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.component.base.RYSwitch
import me.ash.reader.ui.component.base.RadioDialog
import me.ash.reader.ui.component.base.RadioDialogOption
import me.ash.reader.ui.component.base.Subtitle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.ext.getBrowserAppList
import me.ash.reader.ui.page.settings.SettingItem
import me.ash.reader.ui.theme.palette.onLight

@Composable
fun InteractionPage(
    onBack: () -> Unit,
    prefetchViewModel: PrefetchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val prefetchProgress = prefetchViewModel.progress.collectAsStateValue()
    val initialPage = LocalInitialPage.current
    val initialFilter = LocalInitialFilter.current
    val swipeToStartAction = LocalArticleListSwipeStartAction.current
    val swipeToEndAction = LocalArticleListSwipeEndAction.current
    val markAsReadOnScroll = LocalMarkAsReadOnScroll.current
    val hideEmptyGroups = LocalHideEmptyGroups.current
    val fullContentAllFeeds = LocalFullContentAllFeeds.current
    val prefetchImages = LocalPrefetchImages.current
    val prefetchScope = LocalPrefetchScope.current
    val imageCacheSize = LocalImageCacheSize.current
    val sortUnreadArticles = LocalSortUnreadArticles.current
    val pullToSwitchArticle = LocalPullToSwitchArticle.current
    val openLink = LocalOpenLink.current
    val openLinkSpecificBrowser = LocalOpenLinkSpecificBrowser.current
    val sharedContent = LocalSharedContent.current
    val settings = LocalSettings.current
    val pullToSwitchFeed = settings.pullToSwitchFeed

    val scope = rememberCoroutineScope()
    val isOpenLinkSpecificBrowserItemEnabled = remember(openLink) {
        openLink == OpenLinkPreference.SpecificBrowser
    }
    var initialPageDialogVisible by remember { mutableStateOf(false) }
    var initialFilterDialogVisible by remember { mutableStateOf(false) }
    var swipeStartDialogVisible by remember { mutableStateOf(false) }
    var swipeEndDialogVisible by remember { mutableStateOf(false) }
    var openLinkDialogVisible by remember { mutableStateOf(false) }
    var openLinkSpecificBrowserDialogVisible by remember { mutableStateOf(false) }
    var sharedContentDialogVisible by remember { mutableStateOf(false) }
    var prefetchScopeDialogVisible by remember { mutableStateOf(false) }
    var imageCacheSizeDialogVisible by remember { mutableStateOf(false) }
    var showSortUnreadArticlesDialog by remember { mutableStateOf(false) }
    var showPullToLoadDialog by remember { mutableStateOf(false) }

    RYScaffold(
        containerColor = MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface,
        navigationIcon = {
            FeedbackIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onBack
            )
        },
        content = {
            LazyColumn {
                item {
                    DisplayText(text = stringResource(R.string.interaction), desc = "")
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.on_start),
                    )
                    SettingItem(
                        title = stringResource(R.string.initial_page),
                        desc = initialPage.toDesc(context),
                        onClick = {
                            initialPageDialogVisible = true
                        },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.initial_filter),
                        desc = initialFilter.toDesc(context),
                        onClick = {
                            initialFilterDialogVisible = true
                        },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.feeds_page),
                    )
                    SettingItem(
                        title = stringResource(R.string.hide_empty_groups),
                        onClick = {
                            hideEmptyGroups.toggle(context, scope)
                        },
                    ) {
                        RYSwitch(activated = hideEmptyGroups.value) {
                            hideEmptyGroups.toggle(context, scope)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.article_list),
                    )
                    SettingItem(
                        title = stringResource(R.string.swipe_to_start),
                        desc = swipeToStartAction.desc,
                        onClick = {
                            swipeStartDialogVisible = true
                        },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.swipe_to_end),
                        desc = swipeToEndAction.desc,
                        onClick = {
                            swipeEndDialogVisible = true
                        },
                    ) {}

                    SettingItem(
                        title = stringResource(R.string.sort_unread_articles),
                        onClick = {
                            showSortUnreadArticlesDialog = true
                        },
                        desc = sortUnreadArticles.description()
                    ) {
                    }

                    SettingItem(
                        title = stringResource(R.string.mark_as_read_on_scroll),
                        onClick = {
                            markAsReadOnScroll.toggle(context, scope)
                        },
                    ) {
                        RYSwitch(activated = markAsReadOnScroll.value) {
                            markAsReadOnScroll.toggle(context, scope)
                        }
                    }

                    SettingItem(
                        title = stringResource(R.string.pull_from_bottom),
                        desc = pullToSwitchFeed.description(),
                        onClick = {
                            showPullToLoadDialog = true
                        },
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.reading_page),
                    )
                    SettingItem(
                        title = stringResource(id = R.string.pull_to_switch_article),
                        onClick = { pullToSwitchArticle.toggle(context, scope) }) {
                        RYSwitch(activated = pullToSwitchArticle.value) {
                            pullToSwitchArticle.toggle(context, scope)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

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
                                stringResource(R.string.download_progress, it.current, it.total)
                            } ?: stringResource(R.string.download_now_desc),
                        onClick = { prefetchViewModel.downloadNow() },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.external_links),
                    )
                    SettingItem(
                        title = stringResource(R.string.initial_open_app),
                        desc = openLink.toDesc(context),
                        onClick = {
                            openLinkDialogVisible = true
                        },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.open_link_specific_browser),
                        desc = openLinkSpecificBrowser.toDesc(context),
                        enabled = isOpenLinkSpecificBrowserItemEnabled,
                        onClick = {

                            if (isOpenLinkSpecificBrowserItemEnabled) {
                                openLinkSpecificBrowserDialogVisible = true
                            }
                        },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))

                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.share),
                    )
                    SettingItem(
                        title = stringResource(R.string.shared_content),
                        desc = sharedContent.toDesc(context),
                        onClick = {
                            sharedContentDialogVisible = true
                        },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    )

    RadioDialog(
        visible = prefetchScopeDialogVisible,
        title = stringResource(R.string.prefetch_scope),
        options = PrefetchScopePreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == prefetchScope,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        prefetchScopeDialogVisible = false
    }

    RadioDialog(
        visible = imageCacheSizeDialogVisible,
        title = stringResource(R.string.image_cache_size),
        options = ImageCacheSizePreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == imageCacheSize,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        imageCacheSizeDialogVisible = false
    }

    RadioDialog(
        visible = initialPageDialogVisible,
        title = stringResource(R.string.initial_page),
        options = InitialPagePreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == initialPage,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        initialPageDialogVisible = false
    }

    RadioDialog(
        visible = initialFilterDialogVisible,
        title = stringResource(R.string.initial_filter),
        options = InitialFilterPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == initialFilter,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        initialFilterDialogVisible = false
    }

    RadioDialog(
        visible = swipeStartDialogVisible,
        title = stringResource(R.string.swipe_to_start),
        options = SwipeStartActionPreference.values.map {
            RadioDialogOption(
                text = it.desc,
                selected = it == swipeToStartAction,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        swipeStartDialogVisible = false
    }

    RadioDialog(
        visible = swipeEndDialogVisible,
        title = stringResource(R.string.swipe_to_end),
        options = SwipeEndActionPreference.values.map {
            RadioDialogOption(
                text = it.desc,
                selected = it == swipeToEndAction,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        swipeEndDialogVisible = false
    }


    RadioDialog(
        visible = openLinkDialogVisible,
        title = stringResource(R.string.initial_open_app),
        options = OpenLinkPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == openLink,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        openLinkDialogVisible = false
    }

    val browserList = remember(context) {
        context.getBrowserAppList()
    }

    RadioDialog(
        visible = openLinkSpecificBrowserDialogVisible,
        title = stringResource(R.string.open_link_specific_browser),
        options = browserList.map {
            RadioDialogOption(
                text = it.loadLabel(context.packageManager).toString(),
                selected = it.activityInfo.packageName == openLinkSpecificBrowser.packageName,
            ) {
                openLinkSpecificBrowser.copy(packageName = it.activityInfo.packageName)
                    .put(context, scope)
            }
        },
        onDismissRequest = {
            openLinkSpecificBrowserDialogVisible = false
        }
    )

    RadioDialog(
        visible = sharedContentDialogVisible,
        title = stringResource(R.string.shared_content),
        options = SharedContentPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == sharedContent,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        sharedContentDialogVisible = false
    }

    RadioDialog(
        visible = showSortUnreadArticlesDialog,
        title = stringResource(R.string.sort_unread_articles),
        options = SortUnreadArticlesPreference.values.map {
            RadioDialogOption(
                text = it.description(),
                selected = it == sortUnreadArticles,
            ) {
                it.put(context, scope)
            }
        },
        onDismissRequest = {
            showSortUnreadArticlesDialog = false
        }
    )

    RadioDialog(
        visible = showPullToLoadDialog,
        title = stringResource(R.string.pull_from_bottom),
        options = PullToLoadNextFeedPreference.values.map {
            RadioDialogOption(
                text = it.description(),
                selected = it == pullToSwitchFeed,
            ) {
                it.put(context, scope)
            }
        },
        onDismissRequest = {
            showPullToLoadDialog = false
        }
    )
}
