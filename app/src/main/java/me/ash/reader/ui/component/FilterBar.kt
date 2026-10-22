package me.ash.reader.ui.component

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import me.ash.reader.infrastructure.preference.LocalExpressiveStyle
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import android.os.Build
import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.ash.reader.domain.model.general.Filter
import me.ash.reader.infrastructure.preference.FlowFilterBarStylePreference
import me.ash.reader.infrastructure.preference.LocalThemeIndex
import me.ash.reader.ui.ext.surfaceColorAtElevation
import me.ash.reader.ui.theme.palette.onDark

@Composable
fun FilterBar(
    modifier: Modifier = Modifier,
    filter: Filter,
    filterBarStyle: Int,
    filterBarFilled: Boolean,
    filterBarPadding: Dp,
    filterBarTonalElevation: Dp,
    filterOnClick: (Filter) -> Unit = {},
) {
    val view = LocalView.current
    if (LocalExpressiveStyle.current.value) {
        ExpressiveFilterBar(modifier, filter) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            filterOnClick(it)
        }
        return
    }
    val themeIndex = LocalThemeIndex.current
    val indicatorColor = if (themeIndex == 5 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    } onDark MaterialTheme.colorScheme.secondaryContainer

    val containerHeight = when (filterBarStyle) {
        FlowFilterBarStylePreference.Icon.value -> 64.dp
        else -> 80.dp
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(filterBarTonalElevation),
        modifier = modifier
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .defaultMinSize(minHeight = containerHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Spacer(modifier = Modifier.width(filterBarPadding))
            Filter.values.forEach { item ->
                NavigationBarItem(
                    modifier = Modifier.height(containerHeight),
                    alwaysShowLabel = when (filterBarStyle) {
                        FlowFilterBarStylePreference.Icon.value -> false
                        FlowFilterBarStylePreference.IconLabel.value -> true
                        FlowFilterBarStylePreference.IconLabelOnlySelected.value -> false
                        else -> false
                    },
                    icon = {
                        Icon(
                            imageVector = if (filter == item && filterBarFilled) {
                                item.iconFilled
                            } else {
                                item.iconOutline
                            },
                            contentDescription = item.toName()
                        )
                    },
                    label = if (filterBarStyle == FlowFilterBarStylePreference.Icon.value) {
                        null
                    } else {
                        {
                            Text(
                                text = item.toName(),
//                            style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    selected = filter == item,
                    onClick = {
//                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        filterOnClick(item)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = indicatorColor,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedIconColor = MaterialTheme.colorScheme.contentColorFor(indicatorColor),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            Spacer(modifier = Modifier.width(filterBarPadding))
        }
    }
}

/**
 * A floating toolbar instead of a full-width bar: a light pill with a hairline outline and a soft
 * shadow, so it stands off the content; the list fades out behind it. The selected filter is a
 * labelled tonal pill, the others even 48dp icon buttons.
 */
@Composable
private fun ExpressiveFilterBar(modifier: Modifier, filter: Filter, onClick: (Filter) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(0f to Color.Transparent, 0.45f to colors.surface)
                )
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.height(60.dp).padding(6.dp).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Filter.values.forEach { item ->
                    val selected = item == filter
                    val icon =
                        when {
                            // A proper "unread" glyph; the plain dot read as unfinished here.
                            item == Filter.Unread ->
                                if (selected) Icons.Rounded.MarkEmailUnread
                                else Icons.Outlined.MarkEmailUnread
                            selected -> item.iconFilled
                            else -> item.iconOutline
                        }
                    Surface(
                        selected = selected,
                        onClick = { onClick(item) },
                        shape = CircleShape,
                        color = if (selected) colors.primaryContainer else Color.Transparent,
                        contentColor =
                            if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    ) {
                        Row(
                            modifier =
                                Modifier.height(48.dp)
                                    .defaultMinSize(minWidth = 48.dp)
                                    .padding(horizontal = if (selected) 16.dp else 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = if (selected) null else item.toName(),
                                modifier = Modifier.size(22.dp),
                            )
                            if (selected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.toName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
