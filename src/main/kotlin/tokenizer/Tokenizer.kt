package tokenizer

interface Tokenizer {
    fun tokenize(text: String): Collection<Token>
}
