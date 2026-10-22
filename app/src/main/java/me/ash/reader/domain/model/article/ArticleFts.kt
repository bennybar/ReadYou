package me.ash.reader.domain.model.article

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Full-text search index over an article's readable text.
 *
 * The article body the reader actually shows lives either in [Article.rawDescription] or, for
 * prefetched articles, in a file on disk — neither of which the article table can search. This
 * table holds the plain text of both, so search covers the whole article instead of the 280
 * character preview in shortDescription.
 */
@Fts4
@Entity(tableName = "article_fts")
data class ArticleFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int? = null,
    val articleId: String,
    // Title and body together, so a single MATCH covers both.
    val content: String,
)
