package com.mkondratek.verbum.tokenizer

import java.nio.file.Path
import kotlin.io.path.extension

/**
 * Example advanced lexer that handles different programming languages
 * This demonstrates how the enhanced tokenization system supports lexer-style processing
 */
class CodeLexer : Tokenizer {
    
    private val kotlinKeywords = setOf(
        "class", "fun", "val", "var", "if", "else", "when", "for", "while", 
        "return", "import", "package", "object", "interface", "abstract", "override"
    )
    
    private val javaKeywords = setOf(
        "class", "public", "private", "protected", "static", "final", "abstract",
        "if", "else", "for", "while", "return", "import", "package", "interface"
    )
    
    override fun tokenize(text: String): Collection<Token> {
        return tokenize(text, null)
    }
    
    override fun tokenize(text: String, filePath: Path?): Collection<Token> {
        val keywords = when (filePath?.extension?.lowercase()) {
            "kt", "kts" -> kotlinKeywords
            "java" -> javaKeywords
            else -> emptySet()
        }
        
        return parseTokens(text, keywords)
    }
    
    override fun canHandle(filePath: Path): Boolean {
        return filePath.extension.lowercase() in setOf("kt", "kts", "java", "js", "ts", "py", "cpp", "c", "h")
    }
    
    private fun parseTokens(text: String, keywords: Set<String>): List<Token> {
        val tokens = mutableListOf<Token>()
        var line = 1
        var column = 1
        var offset = 0
        
        var i = 0
        while (i < text.length) {
            val char = text[i]
            
            when {
                char.isWhitespace() -> {
                    if (char == '\n') {
                        line++
                        column = 1
                    } else {
                        column++
                    }
                    offset++
                    i++
                }
                
                char.isLetter() || char == '_' -> {
                    val start = i
                    val startColumn = column
                    val startOffset = offset
                    
                    while (i < text.length && (text[i].isLetterOrDigit() || text[i] == '_')) {
                        i++
                        column++
                        offset++
                    }
                    
                    val tokenText = text.substring(start, i)
                    val tokenType = if (keywords.contains(tokenText)) TokenType.KEYWORD else TokenType.IDENTIFIER
                    val position = TokenPosition(line, startColumn, startOffset, tokenText.length)
                    
                    tokens.add(Token(tokenText.lowercase(), tokenType, position))
                }
                
                char.isDigit() -> {
                    val start = i
                    val startColumn = column
                    val startOffset = offset
                    
                    while (i < text.length && (text[i].isDigit() || text[i] == '.')) {
                        i++
                        column++
                        offset++
                    }
                    
                    val tokenText = text.substring(start, i)
                    val position = TokenPosition(line, startColumn, startOffset, tokenText.length)
                    
                    tokens.add(Token(tokenText, TokenType.NUMBER, position))
                }
                
                char == '"' || char == '\'' -> {
                    val quote = char
                    val start = i
                    val startColumn = column
                    val startOffset = offset
                    
                    i++ // skip opening quote
                    column++
                    offset++
                    
                    while (i < text.length && text[i] != quote) {
                        if (text[i] == '\\' && i + 1 < text.length) {
                            i += 2 // skip escaped character
                            column += 2
                            offset += 2
                        } else {
                            if (text[i] == '\n') {
                                line++
                                column = 1
                            } else {
                                column++
                            }
                            offset++
                            i++
                        }
                    }
                    
                    if (i < text.length) {
                        i++ // skip closing quote
                        column++
                        offset++
                    }
                    
                    val tokenText = text.substring(start + 1, minOf(i - 1, text.length))
                    val position = TokenPosition(line, startColumn, startOffset, i - start)
                    
                    if (tokenText.isNotBlank() && tokenText.length >= 2) {
                        tokens.add(Token(tokenText.lowercase(), TokenType.STRING_LITERAL, position))
                    }
                }
                
                char in "+-*/=<>!&|" -> {
                    val position = TokenPosition(line, column, offset, 1)
                    tokens.add(Token(char.toString(), TokenType.OPERATOR, position))
                    i++
                    column++
                    offset++
                }
                
                else -> {
                    // Skip other characters (punctuation, etc.)
                    i++
                    column++
                    offset++
                }
            }
        }
        
        return tokens.filter { 
            it.text.length >= 2 || it.type == TokenType.OPERATOR || it.type == TokenType.PUNCTUATION 
        } // Maintain minimum length requirement but allow operators/punctuation
    }
}
