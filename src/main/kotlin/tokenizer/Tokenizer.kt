package tokenizer

interface Tokenizer {
    // todo: consider Token class
    fun tokenize(text: String): Collection<String>
}
