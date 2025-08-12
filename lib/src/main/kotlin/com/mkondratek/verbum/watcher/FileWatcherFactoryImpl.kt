package com.mkondratek.verbum.watcher

 class FileWatcherFactoryImpl : FileWatcherFactory {
  override fun create() = FileWatcherImpl()
}