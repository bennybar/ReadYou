package me.ash.reader.domain.model.article

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/**
 * Full-text search index over an article's readable text.
 *
 * The article body the reader actually shows lives either in [Article.rawDescription] or, for
 * prefetched articles, in a file on disk — neither of which the article table can search. This
 * table holds the plain text of both, so search covers the whole article instead of the 280
 * character preview in shortDescription.
 */
// unicode61 splits on all Unicode punctuation, not only ASCII, so a Hebrew word next to a curly
// quote or an em dash is still its own token. articleId is stored but not searchable: ids like
// "2$1792…" otherwise matched any search for "2" or "17".
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, notIndexed = ["articleId"])
@Entity(tableName = "article_fts")
data class ArticleFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int? = null,
    val articleId: String,
    // Title and body together, so a single MATCH covers both.
    val content: String,
    /** Hebrew words again without their prefix letters, see [hebrewVariants]. */
    @ColumnInfo(defaultValue = "") val variants: String = "",
)

private val hebrewWord = Regex("[\\u05D0-\\u05EA]{3,}")
private const val HEBREW_PREFIXES = "ובהכלמש"

/**
 * Hebrew attaches "and", "the", "in", "to", "from", "that" and "as" to the front of the next word,
 * so the article says "והממשלה" ("and the government") where a reader searches "ממשלה". Index each
 * such word a second time with up to two leading prefix letters removed (enough for the common
 * pairs וה, שה, מה, וב), keeping at least three letters, so the bare word finds it. Some words
 * simply start with one of these letters, so a few false positives ("שחקים" finding "משחקים") are
 * the price.
 */
fun hebrewVariants(text: String): String =
    hebrewWord
        .findAll(text)
        .flatMap { match ->
            val word = match.value
            (1..2).asSequence()
                .takeWhile { it < word.length - 2 && word[it - 1] in HEBREW_PREFIXES }
                .map { word.substring(it) }
        }
        .distinct()
        .joinToString(" ")

/** The words of a search, as the index splits them: letters and digits only. */
fun searchTerms(content: String): List<String> =
    content.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

/**
 * Turns raw user input into a safe FTS4 MATCH expression.
 *
 * Typed text goes straight into MATCH, and FTS treats characters like `"`, `*`, `:`, `-` and the
 * words AND/OR/NOT as operators, so a search for `c++` or `foo:bar` would throw a malformed-MATCH
 * exception instead of returning results. Keep only letters and digits, and make the final token a
 * prefix so results narrow as the user types.
 */
fun ftsMatchQuery(content: String): String {
    val tokens = searchTerms(content)
    // MATCH has no "match everything" expression, so fall back to something no article contains
    // rather than letting SQLite reject an empty query. Only reached for input like "!!!".
    if (tokens.isEmpty()) return "zzzznomatchzzzz4f2a1"
    return tokens.mapIndexed { i, t -> if (i == tokens.lastIndex) "$t*" else t }.joinToString(" ")
}
