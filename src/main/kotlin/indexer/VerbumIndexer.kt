package indexer

import tokenizer.Tokenizer
import watcher.FileEvent
import watcher.FileWatcher
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

// todo: add thread safety
// todo: handle errors from readText and list
// todo: consider walkFileTree instead of list
class VerbumIndexer(
    private val tokenizer: Tokenizer,
    private val fileWatcher: FileWatcher,
) : Indexer {
    private val index = mutableMapOf<String, MutableSet<Path>>()
    private val indexReverse = mutableMapOf<Path, MutableSet<String>>()

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

    override fun query(word: String): Set<Path> = index[word]?.toSet() ?: emptySet()

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
            Files.list(path).use { stream ->
                stream.forEach {
                    walkAndExecute(it, action)
                }
            }
        } else if (Files.isRegularFile(path)) {
            action(path)
        }
    }

    private fun indexFile(file: Path) {
        val tokens = tokenizer.tokenize(file.readText())
        tokens
            .forEach {
                index.computeIfAbsent(it) { mutableSetOf() }.add(file)
            }

        indexReverse[file] = tokens.toMutableSet()
    }

    private fun deindexFile(file: Path) {
        val tokens = indexReverse.getOrDefault(file, mutableSetOf())
        tokens.forEach {
            val paths = index.getOrDefault(it, mutableSetOf())
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
