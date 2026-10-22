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
 * - The query string is part of the id, minus tracking parameters. YouTube (`/watch?v=…`) and
 *   `article.php?id=…` put the real id there, so without it every video of a channel with a
 *   recurring title keyed the same and all but the first were hidden.
 * - The title alone is not enough, because a daily column keeps its headline; its article id
 *   changes, so requiring both keeps it safe.
 */
fun dedupeKeyOf(link: String, title: String): String? {
    val url = link.toHttpUrlOrNull() ?: return null

    val articleId =
        url.pathSegments.lastOrNull { it.isNotBlank() }?.substringBeforeLast('.')?.takeIf {
            it.isNotBlank()
        } ?: return null

    val query =
        url.queryParameterNames
            .filterNot { name -> TRACKING_PARAMETERS.any { name.startsWith(it) } }
            .sorted()
            .joinToString("&") { name -> "$name=${url.queryParameterValues(name).joinToString(",")}" }

    val normalisedTitle = title.trim().lowercase().replace(WHITESPACE, " ")
    if (normalisedTitle.isBlank()) return null

    return "${url.host}|$articleId?$query|$normalisedTitle"
}

private val TRACKING_PARAMETERS = listOf("utm_", "fbclid", "gclid", "mc_", "ref", "_ga")

private val WHITESPACE = Regex("\\s+")
