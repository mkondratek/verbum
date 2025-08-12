package com.mkondratek.verbum.watcher

fun interface FileWatcherFactory {
  fun create(): FileWatcher
}