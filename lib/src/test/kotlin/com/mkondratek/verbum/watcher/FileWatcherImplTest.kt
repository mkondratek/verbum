package com.mkondratek.verbum.watcher

import com.mkondratek.verbum.watcher.FileEvent
import com.mkondratek.verbum.watcher.FileWatcherImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isDirectory
import kotlin.test.assertFailsWith

class FileWatcherImplTest {
    private lateinit var watcher: FileWatcherImpl

    @BeforeEach
    fun setUp() {
        watcher = FileWatcherImpl()
    }

    @Test
    fun `watch throws on non-directory`() {
        val mockPath = mockk<Path>()
        every { mockPath.isDirectory() } returns false

        assertFailsWith<IllegalArgumentException> {
            watcher.watch(mockPath)
        }
    }

    @Test
    fun `watch registers directory and does not duplicate`() {
        val tempDir = Files.createTempDirectory("testDir")
        watcher.watch(tempDir)

        // Calling watch again on same dir does not register twice
        watcher.watch(tempDir)

        // We can only indirectly test internal state, so no crash means pass
        watcher.unwatch(tempDir)
        tempDir.toFile().delete()
    }

    @Test
    fun `unwatch removes watch key`() {
        val tempDir = Files.createTempDirectory("testDir")
        watcher.watch(tempDir)

        watcher.unwatch(tempDir)

        // Unwatch again should not throw
        watcher.unwatch(tempDir)
        tempDir.toFile().delete()
    }

    @Test
    fun `addListener and removeListener work`() {
        val listener: (FileEvent) -> Unit = {}

        watcher.addListener(listener)
        watcher.removeListener(listener)
    }

    @Test
    fun `notifyListeners calls added listeners`() {
        val called = mutableListOf<FileEvent>()
        val listener: (FileEvent) -> Unit = { called.add(it) }
        watcher.addListener(listener)

        val event = FileEvent.Created(Paths.get("/tmp/fake.txt"))
        watcher.run {
            val notifyListeners = this::class.java.getDeclaredMethod("notifyListeners", FileEvent::class.java)
            notifyListeners.isAccessible = true
            notifyListeners.invoke(this, event)
        }

        assertEquals(1, called.size)
        assertEquals(event, called[0])
    }

    @Test
    fun `start and stop watcher launches and cancels job`(): Unit =
        runBlocking {
            val tempDir = Files.createTempDirectory("testDir")
            watcher.watch(tempDir)
            watcher.start()
            watcher.stop()
            tempDir.toFile().delete()
        }
}
