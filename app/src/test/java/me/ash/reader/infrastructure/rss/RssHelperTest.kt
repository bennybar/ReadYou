package me.ash.reader.infrastructure.rss

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

internal const val enclosureUrlString1: String = "https://example.com/enclosure.jpg"
internal const val enclosureUrlString2: String = "https://github.blog/wp-content/uploads/2024/03/github_copilot_header.png"
internal const val imageUrlString: String = "https://example.com/image.jpg"
internal const val enclosureHtmlCase1: String = """
        <enclosure url="$enclosureUrlString1" type="image/jpeg"/>
        <img src="$imageUrlString"/>
    """
internal const val enclosureHtmlCase2: String = """
        <img src="$imageUrlString"/>
        <enclosure url="$enclosureUrlString1" type="image/jpeg"/>
        <img src="$imageUrlString"/> 
    """
internal const val enclosureHtmlCase3: String = """
        <img src="$imageUrlString"/>
        <enclosure url="$enclosureUrlString2" type="image/png"/>
        <img src="$imageUrlString"/> 
    """
internal const val imageHtmlCase1: String = """
        <img src="$enclosureUrlString1"/>
        <img src="$imageUrlString"/> 
    """
internal const val imageHtmlCase2: String = """
        <img src="$imageUrlString"/> 
        <img src="$enclosureUrlString1"/> 
        <img src="$enclosureUrlString1"/> 
    """

@RunWith(MockitoJUnitRunner::class)
class RssHelperTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockIODispatcher: CoroutineDispatcher

    @Mock
    private lateinit var mockOkHttpClient: OkHttpClient

    private lateinit var rssHelper: RssHelper

    @Before
    fun setUp() {
        mockContext = mock<Context> { }
        mockIODispatcher = mock<CoroutineDispatcher> {}
        mockOkHttpClient = mock<OkHttpClient> {}
        rssHelper = RssHelper(mockContext, mockIODispatcher, mockOkHttpClient)
    }

    @Test
    fun testFindThumbnail() {
        Assert.assertNull(rssHelper.findThumbnail(""))
        Assert.assertNull(rssHelper.findThumbnail(" "))
        Assert.assertNull(rssHelper.findThumbnail(null))
        Assert.assertEquals(enclosureUrlString1, rssHelper.findThumbnail(enclosureHtmlCase1))
        Assert.assertEquals(enclosureUrlString1, rssHelper.findThumbnail(enclosureHtmlCase2))
        Assert.assertEquals(enclosureUrlString2, rssHelper.findThumbnail(enclosureHtmlCase3))
        Assert.assertEquals(enclosureUrlString1, rssHelper.findThumbnail(imageHtmlCase1))
        Assert.assertEquals(imageUrlString, rssHelper.findThumbnail(imageHtmlCase2))
    }

    @Test
    fun testFindThumbnailSkipsDecorativeImages() {
        val case = """
            <p><img src="https://s.w.org/images/core/emoji/15.0.3/72x72/2b50.png" class="wp-smiley"/> Hi</p>
            <img src="https://example.com/spacer.gif"/>
            <img width="1" height="1" src="https://example.com/t.png"/>
            <img data-width="40" src="$imageUrlString"/>
        """
        Assert.assertEquals(imageUrlString, rssHelper.findThumbnail(case))
    }

    @Test
    fun testEnclosureNoFilenameExtension() {
        val case = """
            <enclosure url="$imageUrlString" type="image/jpeg" length="0"/>
        """
        Assert.assertEquals(imageUrlString, rssHelper.findThumbnail(case))
    }

    @Test
    fun testMediaNamespaceThumbnailInRSS20() {
        val case = """
            <enclosure url="$imageUrlString" type="image/jpeg" length="0"/>
        """
        Assert.assertEquals(imageUrlString, rssHelper.findThumbnail(case))
    }
}
