package me.ash.reader.domain.model.article

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DedupeKeyTest {

    /** The real duplicates: Mako carries one story under several sections. */
    @Test
    fun theSameStoryUnderDifferentSectionsGetsOneKey() {
        val title = "המרוץ הישראלי ליירט את מתקפת הכטב\"מים הבאה"

        val tech12 =
            dedupeKeyOf(
                "https://www.mako.co.il/news-money/tech12/Article-dce7a286aed4f91027.htm",
                title,
            )
        val military =
            dedupeKeyOf(
                "https://www.mako.co.il/news-military/2026_q3/Article-dce7a286aed4f91027.htm",
                title,
            )

        assertEquals(tech12, military)
    }

    @Test
    fun theSameStoryUnderTech12AndNexterGetsOneKey() {
        val title = "תוך 3 ימים: מטא מבטלת את השינוי שעורר זעם"

        assertEquals(
            dedupeKeyOf(
                "https://www.mako.co.il/news-money/tech12/Article-8d6259a87155f91027.htm",
                title,
            ),
            dedupeKeyOf("https://www.mako.co.il/nexter-news/Article-8d6259a87155f91027.htm", title),
        )
    }

    /**
     * A daily column keeps its headline but gets a new article id every day. Keying on the title
     * alone would collapse the whole series and hide every edition but the first.
     */
    @Test
    fun aDailyColumnUnderTheSameHeadlineIsNotCollapsed() {
        val title = "מבזקי הבוקר"

        assertNotEquals(
            dedupeKeyOf("https://www.mako.co.il/news/Article-aaaaaaaaaaaaaaa1.htm", title),
            dedupeKeyOf("https://www.mako.co.il/news/Article-bbbbbbbbbbbbbbb2.htm", title),
        )
    }

    @Test
    fun differentSitesAreNeverCollapsed() {
        val title = "Same headline"

        assertNotEquals(
            dedupeKeyOf("https://a.example/news/story-1", title),
            dedupeKeyOf("https://b.example/news/story-1", title),
        )
    }

    /** Nothing identifying in the path, so keying on it would collapse the entire site. */
    @Test
    fun aUrlWithNoIdentifyingPathIsNotKeyed() {
        assertNull(dedupeKeyOf("https://example.com/", "A title"))
        assertNull(dedupeKeyOf("not a url", "A title"))
    }

    @Test
    fun anArticleWithNoTitleIsNotKeyed() {
        assertNull(dedupeKeyOf("https://example.com/news/story-1", "   "))
    }
}
