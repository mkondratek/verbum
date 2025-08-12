package com.mkondratek.verbum.watcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.isDirectory

class FileWatcherImpl : FileWatcher {
    private val watchService = FileSystems.getDefault().newWatchService()
    private val listeners = CopyOnWriteArrayList<(FileEvent) -> Unit>()
    private val watchKeys = ConcurrentHashMap<Path, WatchKey>()

    private var watchJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun watch(path: Path) {
        if (path.isDirectory()) {
            if (watchKeys[path] != null) {
                return
            }

            val key =
                path.register(
                    watchService,
                    ENTRY_CREATE,
                    ENTRY_MODIFY,
                    ENTRY_DELETE,
                )
            watchKeys[path] = key
        } else {
            throw IllegalArgumentException("Can only watch directories")
        }
    }

    override fun unwatch(path: Path) {
        watchKeys[path]?.cancel()
        watchKeys.remove(path)
    }

    override fun start() {
        watchJob =
            scope.launch {
                while (isActive) {
                    val key =
                        try {
                            watchService.take()
                        } catch (e: ClosedWatchServiceException) {
                            break
                        }

                    if (!key.isValid) {
                        continue
                    }

                    val dir = watchKeys.entries.find { it.value == key }?.key
                    if (dir != null) {
                        key.pollEvents().forEach { event ->
                            val kind = event.kind()
                            val relativePath = event.context() as Path
                            val fullPath = dir.resolve(relativePath)

                            val event =
                                when (kind) {
                                    ENTRY_CREATE -> FileEvent.Created(fullPath)
                                    ENTRY_MODIFY -> FileEvent.Modified(fullPath)
                                    ENTRY_DELETE -> FileEvent.Deleted(fullPath)

                                    else -> throw IllegalArgumentException("Unknown event kind")
                                }
                            notifyListeners(event)
                        }
                    }

                    val valid = key.reset()
                    if (!valid) {
                        watchKeys.entries.removeIf { it.value == key }
                    }
                }
            }
    }

    override fun stop() {
        watchJob?.cancel()
        watchService.close()
    }

    override fun addListener(listener: (FileEvent) -> Unit) {
        listeners.add(listener)
    }

    override fun removeListener(listener: (FileEvent) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(event: FileEvent) {
        listeners.toList().forEach { it(event) }
    }
}
