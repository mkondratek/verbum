import indexer.VerbumIndexer
import tokenizer.SimpleTokenizer
import watcher.FileWatcherImpl
import java.nio.file.Path

fun main() {
    val tokenizer = SimpleTokenizer()
    val fileWatcher = FileWatcherImpl()
    val indexer = VerbumIndexer(tokenizer, fileWatcher)

    val path = Path.of("docs")
    println(path)
    indexer.addPath(path)
    indexer.startWatching()

    val results = indexer.query("concurrency")
    println("Files containing 'concurrency': $results")

    indexer.stopWatching()
}
