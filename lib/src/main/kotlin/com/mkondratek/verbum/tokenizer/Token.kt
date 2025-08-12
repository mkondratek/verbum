package com.mkondratek.verbum.tokenizer

data class Token(
    val text: String,
    val type: TokenType = TokenType.WORD,
    val position: TokenPosition? = null,
)

enum class TokenType {
    WORD,           // Regular words
    IDENTIFIER,     // Programming identifiers  
    KEYWORD,        // Programming keywords
    STRING_LITERAL, // String literals
    NUMBER,         // Numeric literals
    OPERATOR,       // Operators (+, -, etc.)
    PUNCTUATION,    // Punctuation marks
    COMMENT,        // Comments
    WHITESPACE,     // Whitespace (usually filtered)
    UNKNOWN         // Fallback for unrecognized tokens
}

data class TokenPosition(
    val line: Int,
    val column: Int,
    val offset: Int,
    val length: Int = 1,
)
