package com.mkondratek.verbum.tokenizer

class SimpleTokenizer : Tokenizer {
    override fun tokenize(text: String): Collection<Token> =
        text
            .split(Regex("\\W+"))
            .filter { it.isNotBlank() }
            .map { Token(it) }
}
