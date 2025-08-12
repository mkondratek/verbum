package com.mkondratek.verbum.tokenizer

import java.nio.file.Path

interface Tokenizer {
    fun tokenize(text: String): Collection<Token>
    
    /**
     * Enhanced tokenization with file context for lexers that need file type information
     * Default implementation delegates to simple tokenize method for backward compatibility
     */
    fun tokenize(text: String, filePath: Path? = null): Collection<Token> = tokenize(text)
    
    /**
     * Check if this tokenizer can handle a specific file type
     * Useful for language-specific lexers
     */
    fun canHandle(filePath: Path): Boolean = true
}
