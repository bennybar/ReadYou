package me.ash.reader.domain.model.article

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * A key identifying the same story arriving more than once.
 *
 * Sites republish one article under several sections, so the same piece turns up two or three times
 * with different URLs. Mako carries a story under `/news-money/tech12/`, `/news-military/` and
 * `/nexter-news/`, every copy pointing at the same article id:
 *
 * ```
 * /news-money/tech12/Article-dce7a286aed4f91027.htm
 * /news-military/2026_q3/Article-dce7a286aed4f91027.htm
 * ```
 *
 * The host, the site's own article id (the last path segment, minus any extension) and the title
 * must *all* match. That is deliberately strict:
 *
 * - The publish timestamp is not part of it, because copies of one story do not always share one —
 *   two of the Mako pairs did, a third was six hours apart.
 * - The article id alone is not enough, because a site whose URLs carry no meaningful last segment
 *   (`?id=123`, a trailing slash) would collapse everything it publishes.
 * - The title alone is not enough, because a daily column keeps its headline; its article id
 *   changes, so requiring both keeps it safe.
 */
fun dedupeKeyOf(link: String, title: String): String? {
    val url = link.toHttpUrlOrNull() ?: return null

    val articleId =
        url.pathSegments.lastOrNull { it.isNotBlank() }?.substringBeforeLast('.')?.takeIf {
            it.isNotBlank()
        } ?: return null

    val normalisedTitle = title.trim().lowercase().replace(WHITESPACE, " ")
    if (normalisedTitle.isBlank()) return null

    return "${url.host}|$articleId|$normalisedTitle"
}

private val WHITESPACE = Regex("\\s+")
