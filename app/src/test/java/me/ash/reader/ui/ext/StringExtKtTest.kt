package me.ash.reader.ui.ext

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StringExtTest {

    @Test
    fun testExtractDomain() {
        Assert.assertEquals(null, "".extractDomain())
        Assert.assertEquals(null, null.extractDomain())
        var case = "https://ash7.io"
        Assert.assertEquals("ash7.io", case.extractDomain())
        case = "ash7.io"
        Assert.assertEquals("ash7.io", case.extractDomain())
        case = "https://ash7.io/blog/hello/"
        Assert.assertEquals("ash7.io", case.extractDomain())
        case = "http://ash7.io/blog/hello/"
        Assert.assertEquals("ash7.io", case.extractDomain())
        case = "file://ash7.io/blog"
        Assert.assertEquals("ash7.io", case.extractDomain())
        case = "file://127.0.0.1/blog"
        Assert.assertEquals("127.0.0.1", case.extractDomain())
        case = "ftp://127.0.0.1"
        Assert.assertEquals("127.0.0.1", case.extractDomain())
    }

    @Test
    fun testIsRtl() {
        // A Hebrew headline opening with a Latin brand is still Hebrew.
        Assert.assertTrue("CISO יקר, הגיע הזמן לדבר על אבטחה".isRtl())
        Assert.assertTrue("Galaxy S26 מגיע לישראל".isRtl())
        // An English headline quoting one Hebrew word is still English.
        Assert.assertFalse("Why the word שלום means more than hello".isRtl())
        // Digits and punctuation don't vote; a tie goes to the first letter.
        Assert.assertTrue("2026: שנה".isRtl())
        Assert.assertTrue("אב ab".isRtl())
        Assert.assertFalse("ab אב".isRtl())
        Assert.assertFalse("12345".isRtl())
        Assert.assertTrue("مرحبا بالعالم".isRtl())
    }
}
