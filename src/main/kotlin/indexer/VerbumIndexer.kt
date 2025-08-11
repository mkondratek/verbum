package indexer

import tokenizer.Token
import tokenizer.Tokenizer
import watcher.FileEvent
import watcher.FileWatcher
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

class VerbumIndexer(
    private val tokenizer: Tokenizer,
    private val fileWatcher: FileWatcher,
) : Indexer {
    private val index = ConcurrentHashMap<Token, MutableSet<Path>>()
    private val indexReverse = ConcurrentHashMap<Path, MutableSet<Token>>()

    init {
        fileWatcher.addListener { event ->
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
    }

    override fun addPath(path: Path) {
        if (path.isDirectory()) {
            fileWatcher.watch(path)
        } else {
            fileWatcher.watch(path.parent)
        }

        walkAndExecute(path) { indexFile(it) }
    }

    override fun removePath(path: Path) {
        if (path.isDirectory()) {
            fileWatcher.unwatch(path)
        }

        indexReverse.keys.filter { it.startsWith(path) }.forEach { deindexFile(it) }
    }

    override fun query(word: String): Set<Path> = index[Token(word)]?.toSet() ?: emptySet()

    override fun startWatching() {
        fileWatcher.start()
    }

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
        val tokens = tokenizer.tokenize(path.readText())
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
