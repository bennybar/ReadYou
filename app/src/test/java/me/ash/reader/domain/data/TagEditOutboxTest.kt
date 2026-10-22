package me.ash.reader.domain.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class TagEditOutboxTest {
    private lateinit var dir: File
    private lateinit var outbox: TagEditOutbox

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("outbox").toFile()
        outbox = TagEditOutbox(mock<Context> { on { filesDir } doReturn dir })
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun laterChangeToTheSameItemReplacesTheEarlierOne() = runBlocking {
        outbox.add(1, listOf(PendingTagEdit("a", "starred", add = true)))
        outbox.add(1, listOf(PendingTagEdit("a", "starred", add = false)))
        assertEquals(listOf(PendingTagEdit("a", "starred", add = false)), outbox.pending(1))
    }

    @Test
    fun survivesAFreshInstanceAndIsPerAccount() = runBlocking {
        outbox.add(1, listOf(PendingTagEdit("a", "read", add = true)))
        val reopened = TagEditOutbox(mock<Context> { on { filesDir } doReturn dir })
        assertEquals(1, reopened.pending(1).size)
        assertTrue(reopened.pending(2).isEmpty())
    }

    @Test
    fun settledDropsOnlyThatTag() = runBlocking {
        outbox.add(
            1,
            listOf(PendingTagEdit("a", "starred", true), PendingTagEdit("a", "readLater", true)),
        )
        outbox.settled(1, listOf("a"), "starred")
        assertEquals(listOf(PendingTagEdit("a", "readLater", true)), outbox.pending(1))
    }
}
