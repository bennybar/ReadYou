package me.ash.reader.infrastructure.html

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadabilityTest {

    /**
     * A real Ynet article. It ships the same photo twice — once in a desktop gallery link, once
     * inside a `<span class="mobileView">` — with the same `src`, and lets CSS hide one of them.
     * Readability throws the stylesheets away, so without de-duplication the reader stacked the
     * same picture on top of itself.
     */
    private val ynetArticle: String
        get() =
            javaClass.classLoader!!
                .getResourceAsStream("ynet_duplicate_image.html")!!
                .bufferedReader()
                .use { it.readText() }

    private val url = "https://www.ynet.co.il/digital/technews/article/ryhxn11meme"

    @Test
    fun theArticleReallyDoesShipTheSameImageTwice() {
        // Guards the premise: if Ynet ever stops duplicating it, the test below proves nothing.
        val sources = Regex("""<img[^>]*?src=["']([^"']+)""").findAll(ynetArticle).map { it.groupValues[1] }.toList()

        assertEquals(2, sources.size)
        assertEquals(1, sources.toSet().size)
    }

    @Test
    fun extractedArticleShowsTheRepeatedImageOnlyOnce() {
        val element = Readability.parseToElement(ynetArticle, url)
        assertNotNull(element)

        val sources = element!!.select("img").map { it.absUrl("src") }.filter { it.isNotBlank() }

        assertTrue("expected the photo to survive extraction", sources.isNotEmpty())
        assertEquals(
            "the same photo must not be rendered twice",
            sources.toSet().size,
            sources.size,
        )
    }
}
