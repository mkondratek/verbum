package com.mkondratek.verbum.watcher

import java.nio.file.Path

interface FileWatcher {
    fun watch(path: Path)

    fun unwatch(path: Path)

    fun start()

    fun stop()

    fun addListener(listener: (FileEvent) -> Unit)

    fun removeListener(listener: (FileEvent) -> Unit)
}
