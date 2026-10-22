package me.ash.reader.ui.page.settings.whatsnew

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTest {
    @Test
    fun comparesVersionsNumerically() {
        assertTrue(newer("0.25.0", "0.24.0"))
        assertTrue(newer("0.10.0", "0.9.9"))
        assertFalse(newer("0.24.0", "0.24.0"))
        assertFalse(newer("0.16 – 0.23", "0.24.0"))
    }

    @Test
    fun changelogIsNewestFirstAndStartsWithTheCurrentVersion() {
        val versions = CHANGELOG.map { it.version }
        assertEquals(versions, versions.sortedWith { a, b -> if (newer(a, b)) -1 else if (newer(b, a)) 1 else 0 })
        val gradle = java.io.File("build.gradle.kts").readText()
        val versionName = Regex("versionName = \"([^\"]+)\"").find(gradle)!!.groupValues[1]
        // Releasing without a What's new entry would show the previous release's notes.
        assertEquals(versionName, CHANGELOG.first().version)
    }
}
