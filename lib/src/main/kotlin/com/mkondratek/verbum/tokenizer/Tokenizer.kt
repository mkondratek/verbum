package com.mkondratek.verbum.tokenizer

interface Tokenizer {
    fun tokenize(text: String): Collection<Token>
}
