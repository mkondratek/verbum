package com.mkondratek.verbum.tokenizer

class SimpleTokenizer : Tokenizer {
    override fun tokenize(text: String): Collection<Token> =
        text
            .lowercase()                    // Normalize to lowercase for case-insensitive search
            .split(Regex("\\W+"))          // Split on non-word characters
            .filter { it.isNotBlank() && it.length >= 2 }  // Filter blanks and single chars
            .map { Token(it) }
}
