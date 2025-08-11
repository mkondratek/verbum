package watcher

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import waitFor
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertTrue

class FileWatcherIntegrationTest {
    private lateinit var watcher: FileWatcherImpl
    private lateinit var testDir: Path

    @BeforeEach
    fun setup() {
        testDir = Files.createTempDirectory("filewatcher-test")
        watcher = FileWatcherImpl()
        watcher.watch(testDir)
        watcher.start()
    }

    @AfterEach
    fun teardown() {
        watcher.stop()
        testDir.toFile().deleteRecursively()
    }

    @Test
    fun `listener receives created modified and deleted events`() =
        runBlocking {
            val events = ConcurrentLinkedQueue<FileEvent>()
            watcher.addListener { events.add(it) }

            val target = testDir.resolve("testfile.txt")

            // create
            Files.writeString(target, "hello")
            waitFor { events.any { it is FileEvent.Created && it.path.endsWith("testfile.txt") } }

            // modify (write again; give OS a tiny pause so modify is observed)
            Files.writeString(target, "hello world")
            waitFor { events.any { it is FileEvent.Modified && it.path.endsWith("testfile.txt") } }

            // delete
            Files.delete(target)
            waitFor { events.any { it is FileEvent.Deleted && it.path.endsWith("testfile.txt") } }

            // final assertions (optional)
            assertTrue(events.any { it is FileEvent.Created })
            assertTrue(events.any { it is FileEvent.Deleted })
            // Modified may be flaky on some platforms; keep it optional or assert if present
        }

    @Test
    fun `events from multiple directories are tracked`(): Unit =
        runBlocking {
            val dir2 = Files.createTempDirectory("filewatcher-test2")
            watcher.watch(dir2)

            val events = ConcurrentLinkedQueue<FileEvent>()
            watcher.addListener { events.add(it) }

            val file1 = testDir.resolve("a.txt")
            val file2 = dir2.resolve("b.txt")

            Files.writeString(file1, "foo")
            Files.writeString(file2, "bar")

            waitFor { events.any { it is FileEvent.Created && it.path == file1 } }
            waitFor { events.any { it is FileEvent.Created && it.path == file2 } }

            assertTrue(events.any { it is FileEvent.Created && it.path == file1 })
            assertTrue(events.any { it is FileEvent.Created && it.path == file2 })

            dir2.toFile().deleteRecursively()
        }

    @Test
    fun `unwatch stops events from that directory`() =
        runBlocking {
            val events = ConcurrentLinkedQueue<FileEvent>()
            watcher.addListener { events.add(it) }

            val target = testDir.resolve("watched.txt")
            Files.writeString(target, "initial")
            waitFor { events.any { it is FileEvent.Created && it.path == target } }

            // Clear old events so we only track new ones
            events.clear()

            // Unwatch and modify the file
            watcher.unwatch(testDir)
            Files.writeString(target, "modified after unwatch")

            // Give it time to (not) register
            Thread.sleep(500)

            assertTrue(
                events.none { it is FileEvent.Modified && it.path == target },
                "No events should be received after unwatch",
            )
        }

    @Test
    fun `stop stops all events`() =
        runBlocking {
            val events = ConcurrentLinkedQueue<FileEvent>()
            watcher.addListener { events.add(it) }

            val target = testDir.resolve("stoptest.txt")
            Files.writeString(target, "before stop")
            waitFor { events.any { it is FileEvent.Created && it.path == target } }

            // Clear so we only track new ones
            events.clear()

            // Stop watcher
            watcher.stop()

            // Try to create a new file after stopping
            val afterStop = testDir.resolve("afterstop.txt")
            Files.writeString(afterStop, "should not be detected")

            // Give some time for potential events (but we expect none)
            Thread.sleep(500)

            assertTrue(
                events.isEmpty(),
                "No events should be received after stop() is called",
            )
        }

    @Test
    fun `multiple listeners receive events`() =
        runBlocking {
            val events1 = ConcurrentLinkedQueue<FileEvent>()
            val events2 = ConcurrentLinkedQueue<FileEvent>()

            watcher.addListener { events1.add(it) }
            watcher.addListener { events2.add(it) }

            val target = testDir.resolve("multilistener.txt")
            Files.writeString(target, "hello listeners")
            waitFor { events1.any { it.path == target } && events2.any { it.path == target } }

            assertTrue(events1.isNotEmpty(), "First listener should receive events")
            assertTrue(
                events2.isNotEmpty(),
                "Second listener should receive events",
            )
        }

    @Test
    fun `unwatch stops events for directory`() =
        runBlocking {
            val events = ConcurrentLinkedQueue<FileEvent>()
            watcher.addListener { events.add(it) }

            val target = testDir.resolve("file-to-watch.txt")
            Files.writeString(target, "initial content")
            waitFor { events.any { it.path == target } } // Should get Created event

            // Clear events and unwatch the directory
            events.clear()
            watcher.unwatch(testDir)

            // Modify file after unwatching
            Files.writeString(target, "modified content")

            // Wait briefly to ensure no events come in
            Thread.sleep(500)

            assertTrue(
                events.isEmpty(),
                "No events should be received after unwatching directory",
            )
        }

    @Test
    fun `adding and removing listeners works correctly`() =
        runBlocking {
            val events1 = ConcurrentLinkedQueue<FileEvent>()
            val listener1: (FileEvent) -> Unit = { events1.add(it) }
            watcher.addListener(listener1)

            val target = testDir.resolve("file.txt")
            Files.writeString(target, "content")
            waitFor { events1.any { it.path == target } }

            // Remove listener1
            watcher.removeListener(listener1)
            events1.clear()

            // Add a new listener
            val events2 = ConcurrentLinkedQueue<FileEvent>()
            val listener2: (FileEvent) -> Unit = { events2.add(it) }
            watcher.addListener(listener2)

            // Modify the file
            Files.writeString(target, "new content")
            waitFor { events2.any { it is FileEvent.Modified && it.path == target } }

            // Assert listener1 did not receive new events
            assertTrue(
                events1.isEmpty(),
                "Listener1 should not receive events after removal",
            )
            // Assert listener2 did receive the event
            assertTrue(events2.any { it is FileEvent.Modified && it.path == target })
        }

    @Test
    fun `watch multiple directories and receive events correctly`(): Unit =
        runBlocking {
            // Create a second temp directory and watch it
            val secondDir = Files.createTempDirectory("filewatcher-test-2")
            watcher.watch(secondDir)

            val events = ConcurrentLinkedQueue<FileEvent>()
            watcher.addListener { events.add(it) }

            val fileInFirstDir = testDir.resolve("file1.txt")
            val fileInSecondDir = secondDir.resolve("file2.txt")

            // Create files in both directories
            Files.writeString(fileInFirstDir, "first dir file")
            Files.writeString(fileInSecondDir, "second dir file")

            waitFor {
                events.any { it is FileEvent.Created && it.path == fileInFirstDir } &&
                    events.any { it is FileEvent.Created && it.path == fileInSecondDir }
            }

            // Clean up secondDir
            secondDir.toFile().deleteRecursively()
        }

    @Test
    fun `unwatch stops events for that directory only`(): Unit =
        runBlocking {
            val events = ConcurrentLinkedQueue<FileEvent>()
            watcher.addListener { events.add(it) }

            // Create a second directory to watch
            val secondDir = Files.createTempDirectory(testDir.parent, "second-dir")
            watcher.watch(secondDir)

            // Create files in both directories
            val fileInFirstDir = testDir.resolve("file1.txt")
            val fileInSecondDir = secondDir.resolve("file2.txt")

            // Create initial files
            Files.writeString(fileInFirstDir, "initial")
            Files.writeString(fileInSecondDir, "initial")

            waitFor { events.any { it is FileEvent.Created && it.path == fileInFirstDir } }
            waitFor { events.any { it is FileEvent.Created && it.path == fileInSecondDir } }

            // Clear events
            events.clear()

            // Unwatch first directory
            watcher.unwatch(testDir)

            // Trigger modifications in both directories
            Files.writeString(fileInFirstDir, "modified")
            Files.writeString(fileInSecondDir, "modified")

            // Wait and verify that no events come from firstDir, but do come from secondDir
            Thread.sleep(500) // short wait for events propagation

            assertTrue(
                events.none { it.path.startsWith(testDir) },
                "No events should come from unwatched directory",
            )
            assertTrue(
                events.any { it.path.startsWith(secondDir) },
                "Events should come from watched directory",
            )

            // Cleanup second directory
            secondDir.toFile().deleteRecursively()
        }

    @Test
    fun `watching a non-directory path throws exception`() {
        val file = testDir.resolve("regularFile.txt")
        Files.writeString(file, "content")

        val exception =
            runCatching {
                watcher.watch(file)
            }.exceptionOrNull()

        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("Can only watch directories") == true)
    }
}
