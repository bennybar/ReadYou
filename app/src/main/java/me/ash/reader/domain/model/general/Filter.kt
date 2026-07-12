package me.ash.reader.domain.model.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import me.ash.reader.R
import me.ash.reader.domain.model.general.Filter.Companion.All
import me.ash.reader.domain.model.general.Filter.Companion.Starred
import me.ash.reader.domain.model.general.Filter.Companion.Unread

/**
 * Indicates filter conditions.
 *
 * - [All]: all items
 * - [Unread]: unread items
 * - [Starred]: starred items
 */
class Filter private constructor(
    val index: Int,
    val iconOutline: ImageVector,
    val iconFilled: ImageVector,
) {

    fun isStarred(): Boolean = this == Starred
    fun isReadLater(): Boolean = this == ReadLater
    fun isUnread(): Boolean = this == Unread
    fun isAll(): Boolean = this == All

    @Stable
    @Composable
    fun toName(): String = when (this) {
        Unread -> stringResource(R.string.unread)
        Starred -> stringResource(R.string.starred)
        ReadLater -> stringResource(R.string.read_later)
        else -> stringResource(R.string.all)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Stable
    @Composable
    fun toDesc(important: Int): String = when (this) {
        Starred -> pluralStringResource(R.plurals.starred_desc, important, important)
        ReadLater -> pluralStringResource(R.plurals.read_later_desc, important, important)
        Unread -> pluralStringResource(R.plurals.unread_desc, important, important)
        else -> pluralStringResource(R.plurals.all_desc, important, important)
    }

    companion object {

        val Starred = Filter(
            index = 0,
            iconOutline = Icons.Rounded.StarOutline,
            iconFilled = Icons.Rounded.Star,
        )
        val Unread = Filter(
            index = 1,
            iconOutline = Icons.Outlined.FiberManualRecord,
            iconFilled = Icons.Rounded.FiberManualRecord,
        )
        val All = Filter(
            index = 2,
            iconOutline = Icons.AutoMirrored.Rounded.Subject,
            iconFilled = Icons.AutoMirrored.Rounded.Subject,
        )
        val ReadLater = Filter(
            index = 3,
            iconOutline = Icons.Rounded.BookmarkBorder,
            iconFilled = Icons.Rounded.Bookmark,
        )
        val values = listOf(Starred, ReadLater, Unread, All)
    }
}
