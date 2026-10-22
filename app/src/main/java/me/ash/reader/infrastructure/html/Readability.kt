package me.ash.reader.infrastructure.html

import android.util.Log
import net.dankito.readability4j.extended.Readability4JExtended
import net.dankito.readability4j.extended.processor.PostprocessorExtended
import net.dankito.readability4j.extended.util.RegExUtilExtended
import net.dankito.readability4j.model.ReadabilityOptions
import net.dankito.readability4j.processor.MetadataParser
import net.dankito.readability4j.processor.Preprocessor
import org.jsoup.nodes.Element

object Readability {

    fun parseToText(htmlContent: String?, uri: String?): String {
        htmlContent ?: return ""
        return try {
            Readability4JExtended(uri, htmlContent).parse().textContent?.trim() ?: ""
        } catch (e: Exception) {
            Log.e("RLog", "Readability.parseToText '$uri' is error: ", e)
            ""
        }
    }

    fun parseToElement(htmlContent: String?, uri: String?): Element? {
        htmlContent ?: return null
        return Readability4JExtended(uri, htmlContent).parse().articleContent?.also {
            it.removeDuplicateImages()
        }
    }

    /**
     * Drops repeats of an image that is already shown earlier in the article.
     *
     * Sites routinely emit the same photo more than once and let CSS decide which copy is visible —
     * Ynet, for instance, ships one copy in a desktop gallery link and another inside a
     * `<span class="mobileView">`, both with the same `src`. Readability throws the stylesheets
     * away, so every copy survives and the reader shows the photo twice, stacked.
     */
    private fun Element.removeDuplicateImages() {
        val seen = mutableSetOf<String>()
        select("img").forEach { img ->
            val src = img.absUrl("src").ifBlank { img.attr("src") }
            if (src.isBlank()) return@forEach
            if (!seen.add(src)) {
                // Take the wrapper with it when it exists only to hold this image, otherwise an
                // empty <span>/<figure> is left behind where the picture used to be.
                val parent = img.parent()
                img.remove()
                if (parent != null && parent.children().isEmpty() && !parent.hasText()) {
                    parent.remove()
                }
            }
        }
    }

    private fun Readability4JExtended(uri: String?, html: String): Readability4JExtended {
        val options = ReadabilityOptions()
        val regExUtil = RegExUtilExtended()
        return Readability4JExtended(
            uri = uri ?: "",
            html = html,
            options = options,
            regExUtil = regExUtil,
            preprocessor = Preprocessor(regExUtil),
            metadataParser = MetadataParser(regExUtil),
            articleGrabber = RYArticleGrabberExtended(options, regExUtil),
            postprocessor = PostprocessorExtended(),
        )
    }
}
