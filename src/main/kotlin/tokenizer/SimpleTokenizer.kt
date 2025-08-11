package tokenizer

class SimpleTokenizer : Tokenizer {
    override fun tokenize(text: String): Collection<String> = text.split(Regex("\\W+")).filter { it.isNotBlank() }
}
