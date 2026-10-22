package me.ash.reader.domain.model.article

import androidx.room.*
import me.ash.reader.domain.model.feed.Feed
import java.util.*

/**
 * TODO: Add class description
 */
@Entity(
    tableName = "article",
    foreignKeys = [ForeignKey(
        entity = Feed::class,
        parentColumns = ["id"],
        childColumns = ["feedId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["accountId", "dedupeKey"])]
)
data class Article(
    @PrimaryKey
    var id: String,
    @ColumnInfo
    var date: Date,
    @ColumnInfo
    var title: String,
    @ColumnInfo
    var author: String? = null,
    @ColumnInfo
    var rawDescription: String,
    @ColumnInfo
    var shortDescription: String,
    @ColumnInfo
    @Deprecated("fullContent is the same as rawDescription")
    var fullContent: String? = null,
    @ColumnInfo
    var img: String? = null,
    @ColumnInfo
    var link: String,
    @ColumnInfo(index = true)
    var feedId: String,
    @ColumnInfo(index = true)
    var accountId: Int,
    @ColumnInfo
    var isUnread: Boolean = true,
    @ColumnInfo
    var isStarred: Boolean = false,
    @ColumnInfo
    var isReadLater: Boolean = false,
    @ColumnInfo
    var updateAt: Date? = null,
    /**
     * Identifies the same story arriving more than once. Sites republish one article under several
     * sections, so the same piece turns up two or three times with different URLs — Mako, for
     * instance, carries a story under /tech12/, /news-military/ and /nexter-news/, all pointing at
     * the same article id.
     *
     * Host, the site's own article id from the URL, and the title must all match, which is
     * deliberately strict: a site republishing a daily column under the same headline keeps a
     * distinct article id, so it is not collapsed.
     */
    @ColumnInfo(defaultValue = "NULL")
    var dedupeKey: String? = null,
    /** True for every copy after the first, which is the one the app shows. */
    @ColumnInfo(defaultValue = "0")
    var isDuplicate: Boolean = false,
) {

    @Ignore
    var dateString: String? = null
}
