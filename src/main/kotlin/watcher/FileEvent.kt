package watcher

import java.nio.file.Path

sealed class FileEvent(
    val path: Path,
) {
    class Created(
        path: Path,
    ) : FileEvent(path)

    class Modified(
        path: Path,
    ) : FileEvent(path)

    class Deleted(
        path: Path,
    ) : FileEvent(path)
}
