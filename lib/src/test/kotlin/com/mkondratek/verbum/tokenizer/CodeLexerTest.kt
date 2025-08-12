package com.mkondratek.verbum.tokenizer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Paths

class CodeLexerTest {
    
    private val lexer = CodeLexer()
    
    @Test
    fun `tokenize simple kotlin code with keywords and identifiers`() {
        val code = "class MyClass { fun myMethod() { val name = \"hello\" } }"
        val kotlinFile = Paths.get("Test.kt")
        
        val tokens = lexer.tokenize(code, kotlinFile)
        
        val tokenTexts = tokens.map { it.text to it.type }
        
        assertTrue(tokenTexts.contains("class" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("myclass" to TokenType.IDENTIFIER))
        assertTrue(tokenTexts.contains("fun" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("mymethod" to TokenType.IDENTIFIER))
        assertTrue(tokenTexts.contains("val" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("name" to TokenType.IDENTIFIER))
        assertTrue(tokenTexts.contains("hello" to TokenType.STRING_LITERAL))
    }
    
    @Test
    fun `tokenize java code with different keywords`() {
        val code = "public class JavaClass { private static final int value = 42; }"
        val javaFile = Paths.get("Test.java")
        
        val tokens = lexer.tokenize(code, javaFile)
        
        val tokenTexts = tokens.map { it.text to it.type }
        
        assertTrue(tokenTexts.contains("public" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("class" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("javaclass" to TokenType.IDENTIFIER))
        assertTrue(tokenTexts.contains("private" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("static" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("final" to TokenType.KEYWORD))
        assertTrue(tokenTexts.contains("int" to TokenType.IDENTIFIER))
        assertTrue(tokenTexts.contains("value" to TokenType.IDENTIFIER))
        assertTrue(tokenTexts.contains("42" to TokenType.NUMBER))
    }
    
    @Test
    fun `tokenize preserves position information`() {
        val code = "fun test() {\n  val myVar = 42\n}"
        val kotlinFile = Paths.get("Test.kt")
        
        val tokens = lexer.tokenize(code, kotlinFile)
        
        val funToken = tokens.first { it.text == "fun" }
        assertNotNull(funToken.position)
        assertEquals(1, funToken.position!!.line)
        assertEquals(1, funToken.position!!.column)
        assertEquals(0, funToken.position!!.offset)
        
        val myVarToken = tokens.first { it.text == "myvar" }
        assertNotNull(myVarToken.position)
        assertEquals(2, myVarToken.position!!.line)
        assertTrue(myVarToken.position!!.column > 1)
    }
    
    @Test
    fun `canHandle returns true for supported file types`() {
        assertTrue(lexer.canHandle(Paths.get("test.kt")))
        assertTrue(lexer.canHandle(Paths.get("test.java")))
        assertTrue(lexer.canHandle(Paths.get("test.js")))
        assertTrue(lexer.canHandle(Paths.get("test.py")))
        assertTrue(lexer.canHandle(Paths.get("test.cpp")))
        
        assertFalse(lexer.canHandle(Paths.get("test.txt")))
        assertFalse(lexer.canHandle(Paths.get("test.xml")))
    }
    
    @Test
    fun `tokenize with no file path falls back to generic behavior`() {
        val code = "class TestClass { fun method() }"
        
        val tokens = lexer.tokenize(code, null)
        
        // Without file context, should still tokenize but may not distinguish keywords as well
        assertTrue(tokens.any { it.text == "class" })
        assertTrue(tokens.any { it.text == "testclass" })
        assertTrue(tokens.any { it.text == "method" })
    }
    
    @Test
    fun `respects minimum token length requirement`() {
        val code = "a bb c dd e ff"
        
        val tokens = lexer.tokenize(code)
        
        // Single character tokens should be filtered out
        assertFalse(tokens.any { it.text == "a" })
        assertFalse(tokens.any { it.text == "c" })
        assertFalse(tokens.any { it.text == "e" })
        
        // Two character tokens should be included
        assertTrue(tokens.any { it.text == "bb" })
        assertTrue(tokens.any { it.text == "dd" })
        assertTrue(tokens.any { it.text == "ff" })
    }
    
    @Test
    fun `handles string literals correctly`() {
        val code = """val message = "Hello World" val char = 'x'"""
        
        val tokens = lexer.tokenize(code)
        
        val stringTokens = tokens.filter { it.type == TokenType.STRING_LITERAL }
        assertTrue(stringTokens.any { it.text == "hello world" })
        // Single char 'x' should be filtered out due to min length requirement
        assertFalse(stringTokens.any { it.text == "x" })
    }
    
    @Test
    fun `handles operators and numbers`() {
        val code = "val result = xy + 42 * 3.14"
        
        val tokens = lexer.tokenize(code)
        
        assertTrue(tokens.any { it.type == TokenType.OPERATOR && it.text == "+" })
        assertTrue(tokens.any { it.type == TokenType.OPERATOR && it.text == "*" })
        assertTrue(tokens.any { it.type == TokenType.NUMBER && it.text == "42" })
        assertTrue(tokens.any { it.type == TokenType.NUMBER && it.text == "3.14" })
    }
}
