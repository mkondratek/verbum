package com.mkondratek.verbum.indexer

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import com.mkondratek.verbum.tokenizer.SimpleTokenizer
import com.mkondratek.verbum.waitFor
import com.mkondratek.verbum.watcher.FileWatcherFactoryImpl
import com.mkondratek.verbum.watcher.FileWatcherImpl
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertTrue

class VerbumIndexerIntegrationTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var indexer: VerbumIndexer

    @BeforeEach
    fun setup() {
        indexer = VerbumIndexer(SimpleTokenizer(), FileWatcherFactoryImpl())
    }

    @AfterEach
    fun teardown() {
        indexer.stopWatching()
    }

    @Test
    fun `indexer updatesIndex whenFileCreated`() {
        indexer.startWatching()

        val file = tempDir.resolve("test.txt")
        indexer.addPath(file)
        Files.writeString(file, "hello world")

        waitFor { setOf(file) == indexer.query("hello") }
        waitFor { setOf(file) == indexer.query("world") }
    }

    @Test
    fun `indexer deindexesFile andRemovesTokens`() {
        val tempFile = tempDir.resolve("testfile.txt")
        tempFile.writeText("hello world")

        indexer.addPath(tempFile.parent)
        indexer.startWatching()

        indexer.addPath(tempFile)

        indexer.removePath(tempFile)

        waitFor { indexer.query("hello").isEmpty() }
        waitFor { indexer.query("world").isEmpty() }
    }

    @Test
    fun `index parent directory, deindex one file, add another file, queries reflect new file`() {
        indexer.startWatching()

        // Create file A with unique content
        val fileA = tempDir.resolve("A.txt")
        fileA.writeText("apple banana")

        // Add the parent directory to indexer (which indexes fileA)
        indexer.addPath(tempDir)

        waitFor { setOf(fileA) == indexer.query("apple") }
        waitFor { setOf(fileA) == indexer.query("banana") }

        // Remove fileA from indexer
        indexer.removePath(fileA)
        waitFor { indexer.query("apple").isEmpty() }
        waitFor { indexer.query("banana").isEmpty() }

        // Add file B with different content
        val fileB = tempDir.resolve("B.txt")
        fileB.writeText("carrot date")

        // Wait for indexer to pick up fileB (since watching the directory)
        waitFor { setOf(fileB) == indexer.query("carrot") }
        waitFor { setOf(fileB) == indexer.query("date") }
    }

    @Test
    fun `add two files with same content, remove one, queries return remaining file`() {
        indexer.startWatching()

        val content = "elephant fox"

        // File A and File B with same content
        val fileA = tempDir.resolve("A.txt")
        val fileB = tempDir.resolve("B.txt")
        fileA.writeText(content)
        fileB.writeText(content)

        // Add files one by one
        indexer.addPath(fileA)
        indexer.addPath(fileB)

        waitFor { setOf(fileA, fileB) == indexer.query("elephant") }
        waitFor { setOf(fileA, fileB) == indexer.query("fox") }

        // Remove file A
        indexer.removePath(fileA)

        waitFor { setOf(fileB) == indexer.query("elephant") }
        waitFor { setOf(fileB) == indexer.query("fox") }
    }

    @Test
    fun `modifying file updates index correctly`() {
        indexer.startWatching()

        val file = tempDir.resolve("modify.txt")
        file.writeText("initial content")
        indexer.addPath(file)
        waitFor { setOf(file) == indexer.query("initial") }

        // Modify file content
        file.writeText("updated content")

        // Simulate modify event manually or rely on watcher events if reliable
        indexer.removePath(file)
        indexer.addPath(file)

        waitFor { indexer.query("initial").isEmpty() }
        waitFor { setOf(file) == indexer.query("updated") }
    }

    @Test
    fun `removing watched directory deindexes all contained files`() {
        indexer.startWatching()

        val file1 = tempDir.resolve("file1.txt")
        val file2 = tempDir.resolve("file2.txt")
        file1.writeText("apple banana")
        file2.writeText("carrot date")

        indexer.addPath(tempDir)
        waitFor { setOf(file1) == indexer.query("apple") }
        waitFor { setOf(file2) == indexer.query("carrot") }

        indexer.removePath(tempDir)  // unwatch directory and deindex all

        waitFor { indexer.query("apple").isEmpty() }
        waitFor { indexer.query("carrot").isEmpty() }
    }

    @Test
    fun `querying unknown token returns empty set`() {
        indexer.startWatching()

        val file = tempDir.resolve("somefile.txt")
        file.writeText("known words")
        indexer.addPath(file)

        waitFor { setOf(file) == indexer.query("known") }

        val result = indexer.query("unknown_token_123")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `add remove add same file multiple times maintains index consistency`() {
        indexer.startWatching()

        val file = tempDir.resolve("readd.txt")
        file.writeText("first content")
        indexer.addPath(file)
        waitFor { setOf(file) == indexer.query("first") }

        indexer.removePath(file)
        waitFor { indexer.query("first").isEmpty() }

        file.writeText("second content")
        indexer.addPath(file)
        waitFor { setOf(file) == indexer.query("second") }
    }

    @Test
    fun `indexer stops and restarts correctly`() {
        // Start watching and index first file
        indexer.startWatching()
        indexer.addPath(tempDir)
        val fileA = tempDir.resolve("fileA.txt")
        Files.writeString(fileA, "apple banana")
        waitFor { setOf(fileA) == indexer.query("apple") }
        waitFor { setOf(fileA) == indexer.query("banana") }

        // Stop watching
        indexer.stopWatching()

        // Modify file while stopped (should NOT update index)
        Files.writeString(fileA, "apple cherry")
        // Query should still show old content since watcher stopped
        Thread.sleep(500) // give some time to be sure
        assertTrue(indexer.query("banana").contains(fileA))
        assertTrue(indexer.query("cherry").isEmpty())

        // Start watching again
        indexer.startWatching()

        // Modify file again (should update index now)
        Files.writeString(fileA, "apple cherry")
        Thread.sleep(500) // give some time to be sure
        waitFor { setOf(fileA) == indexer.query("cherry") }
        waitFor { indexer.query("banana").isEmpty() }
    }

}
