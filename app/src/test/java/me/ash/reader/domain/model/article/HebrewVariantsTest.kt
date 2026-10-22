package me.ash.reader.domain.model.article

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HebrewVariantsTest {

    @Test
    fun stripsPrefixLettersSoTheBareWordMatches() {
        val variants = hebrewVariants("והממשלה החליטה").split(" ")
        assertTrue("ממשלה" in variants)
        assertTrue("הממשלה" in variants)
    }

    @Test
    fun keepsAtLeastThreeLettersAndIgnoresOtherScripts() {
        // "בית" is only three letters, so stripping ב would leave two: nothing to add.
        assertEquals("", hebrewVariants("בית"))
        assertEquals("", hebrewVariants("Hello world 2026"))
    }

    @Test
    fun onlyStripsWhileTheLettersArePrefixes() {
        // ר is not a prefix letter, so "רכבת" stays as it is.
        assertEquals("", hebrewVariants("רכבת"))
        assertEquals("רכבת", hebrewVariants("ברכבת"))
    }
}
