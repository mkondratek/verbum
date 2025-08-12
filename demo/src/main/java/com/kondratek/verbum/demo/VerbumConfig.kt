package com.kondratek.verbum.demo

import com.mkondratek.verbum.indexer.VerbumIndexer
import com.mkondratek.verbum.tokenizer.SimpleTokenizer
import com.mkondratek.verbum.tokenizer.Tokenizer
import com.mkondratek.verbum.watcher.FileWatcherFactory
import com.mkondratek.verbum.watcher.FileWatcherFactoryImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class VerbumConfig {
    @Bean
    open fun verbumTokenizer(): Tokenizer = SimpleTokenizer()

    @Bean
    open fun verbumFileWatcherFactory(): FileWatcherFactory = FileWatcherFactoryImpl()

    @Bean
    open fun verbumIndexer(
        verbumTokenizer: Tokenizer,
        verbumFileWatcherFactory: FileWatcherFactory,
    ): VerbumIndexer = VerbumIndexer(verbumTokenizer, verbumFileWatcherFactory)
}
