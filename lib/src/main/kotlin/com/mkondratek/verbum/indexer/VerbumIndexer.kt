package com.mkondratek.verbum.indexer

import com.mkondratek.verbum.tokenizer.Token
import com.mkondratek.verbum.tokenizer.Tokenizer
import com.mkondratek.verbum.watcher.FileEvent
import com.mkondratek.verbum.watcher.FileWatcher
import com.mkondratek.verbum.watcher.FileWatcherFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class VerbumIndexer(
  private val tokenizer: Tokenizer,
  private val fileWatcherFactory: FileWatcherFactory,
) : Indexer {
    @Volatile
    private var fileWatcher: FileWatcher

    private val index = ConcurrentHashMap<Token, MutableSet<Path>>()
    private val indexReverse = ConcurrentHashMap<Path, MutableSet<Token>>()
    private val fileEventListener: (FileEvent) -> Unit = { event ->
        when (event) {
            is FileEvent.Created -> {
                if (Files.isRegularFile(event.path)) {
                    indexFile(event.path)
                }
            }

            is FileEvent.Modified -> {
                deindexFile(event.path)
                indexFile(event.path)
            }

            is FileEvent.Deleted -> {
                if (Files.isRegularFile(event.path)) {
                    deindexFile(event.path)
                } else {
                    indexReverse.keys
                        .filter { it.startsWith(event.path) }
                        .forEach { deindexFile(it) }
                }
            }
        }
    }

    init {
        fileWatcher = fileWatcherFactory.create()
        fileWatcher.addListener(fileEventListener)
    }

    override fun addPath(path: Path) {
        if (path.isDirectory()) {
            fileWatcher.watch(path)
        } else if (path.isRegularFile()) {
            fileWatcher.watch(path.parent)
        } else {
            throw IllegalArgumentException("Can only watch directories and files. Got: $path")
        }

        walkAndExecute(path) { indexFile(it) }
    }

    override fun removePath(path: Path) {
        if (path.isDirectory()) {
            fileWatcher.unwatch(path)
        }

        indexReverse.keys.filter { it.startsWith(path) }.forEach { deindexFile(it) }
    }

    override fun query(word: String): Set<Path> = index[Token(word.lowercase())]?.toSet() ?: emptySet()

    @Synchronized
    override fun startWatching() {
        fileWatcher = fileWatcherFactory.create()
        fileWatcher.addListener(fileEventListener)
        fileWatcher.start()
        indexReverse.keys.filter { it.isDirectory() }.forEach { fileWatcher.watch(it) }
        index.clear()
        indexReverse.keys.forEach { indexFile(it) }
    }

    @Synchronized
    override fun stopWatching() {
        fileWatcher.stop()
    }

    private fun walkAndExecute(
        path: Path,
        action: (Path) -> Unit,
    ) {
        if (path.isDirectory()) {
            // todo: consider using Files.walk here or smth else
            Files.list(path).use { stream ->
                stream.forEach {
                    walkAndExecute(it, action)
                }
            }
        } else if (Files.isRegularFile(path)) {
            action(path)
        }
    }

    private fun indexFile(path: Path) {
        val tokens = tokenizer.tokenize(path.readText(), path)
        tokens
            .forEach {
                index.computeIfAbsent(it) { ConcurrentHashMap.newKeySet() }.add(path)
            }

        indexReverse[path] = ConcurrentHashMap.newKeySet<Token>().apply { addAll(tokens) }
    }

    private fun deindexFile(file: Path) {
        val tokens = indexReverse.getOrDefault(file, ConcurrentHashMap.newKeySet())
        tokens.forEach {
            val paths = index.getOrDefault(it, ConcurrentHashMap.newKeySet())
            paths.remove(file)
            if (paths.isEmpty()) {
                index.remove(it)
            } else {
                index.put(it, paths)
            }
        }

        indexReverse.remove(file)
    }
}
