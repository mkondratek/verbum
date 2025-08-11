package indexer

import java.nio.file.Path

interface Indexer {
    fun addPath(path: Path)

    fun removePath(path: Path)

    fun query(word: String): Set<Path>

    fun startWatching()

    fun stopWatching()
}
