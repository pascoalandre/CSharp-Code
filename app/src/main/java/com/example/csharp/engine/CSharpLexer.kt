package com.example.csharp.engine

enum class TokenType {
    // Keywords
    KEYWORD,
    TYPE_KEYWORD,
    
    // Identifiers & Literals
    IDENTIFIER,
    TYPE_IDENTIFIER,
    METHOD_IDENTIFIER,
    NUMBER_LITERAL,
    STRING_LITERAL,
    CHAR_LITERAL,
    BOOLEAN_LITERAL,
    NULL_LITERAL,
    
    // Operators & Punctuation
    OPERATOR,
    PUNCTUATION,
    
    // Comments & Formatting
    COMMENT,
    WHITESPACE,
    UNKNOWN,
    EOF
}

data class Token(
    val type: TokenType,
    val text: String,
    val start: Int,
    val end: Int,
    val line: Int,
    val column: Int
)

class CSharpLexer(private val source: String) {
    private var index = 0
    private var line = 1
    private var column = 1

    companion object {
        val KEYWORDS = setOf(
            "abstract", "as", "async", "await", "base", "break", "case", "catch", "checked",
            "class", "const", "continue", "default", "delegate", "do", "else", "enum", "event",
            "explicit", "extern", "finally", "fixed", "for", "foreach", "goto", "if", "implicit",
            "in", "interface", "internal", "is", "lock", "namespace", "new", "operator", "out",
            "override", "params", "private", "protected", "public", "readonly", "ref", "return",
            "sealed", "sizeof", "stackalloc", "static", "struct", "switch", "this", "throw",
            "try", "typeof", "unchecked", "unsafe", "using", "virtual", "void", "volatile",
            "while", "yield", "var", "record", "init", "get", "set"
        )

        val TYPE_KEYWORDS = setOf(
            "bool", "byte", "char", "decimal", "double", "float", "int", "long", "object",
            "sbyte", "short", "string", "uint", "ulong", "ushort"
        )

        val KNOWN_TYPES = setOf(
            "Console", "Math", "String", "Int32", "Double", "Boolean", "Char", "List",
            "Dictionary", "Array", "DateTime", "TimeSpan", "Task", "Thread", "Exception",
            "Convert", "StringBuilder", "Program", "Enumerable", "Linq"
        )

        val KNOWN_METHODS = setOf(
            "WriteLine", "Write", "ReadLine", "Clear", "ToString", "Parse", "TryParse",
            "Add", "Remove", "Count", "Length", "ToUpper", "ToLower", "Substring",
            "Contains", "IndexOf", "Split", "Trim", "Replace", "Sqrt", "Pow", "Abs",
            "Max", "Min", "Floor", "Ceiling", "Round", "Now", "UtcNow"
        )
    }

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (index < source.length) {
            val token = nextToken()
            if (token != null) {
                tokens.add(token)
            }
        }
        return tokens
    }

    private fun peek(offset: Int = 0): Char? {
        val pos = index + offset
        return if (pos < source.length) source[pos] else null
    }

    private fun advance(): Char {
        val c = source[index++]
        if (c == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return c
    }

    private fun nextToken(): Token? {
        if (index >= source.length) return null

        val start = index
        val startLine = line
        val startCol = column
        val c = peek() ?: return null

        // 1. Whitespace
        if (c.isWhitespace()) {
            val sb = java.lang.StringBuilder()
            while (index < source.length && source[index].isWhitespace()) {
                sb.append(advance())
            }
            return Token(TokenType.WHITESPACE, sb.toString(), start, index, startLine, startCol)
        }

        // 2. Comments or Division
        if (c == '/') {
            val next = peek(1)
            if (next == '/') {
                // Line comment
                val sb = java.lang.StringBuilder()
                while (index < source.length && source[index] != '\n') {
                    sb.append(advance())
                }
                return Token(TokenType.COMMENT, sb.toString(), start, index, startLine, startCol)
            } else if (next == '*') {
                // Block comment
                val sb = java.lang.StringBuilder()
                sb.append(advance()) // /
                sb.append(advance()) // *
                while (index < source.length) {
                    if (source[index] == '*' && peek(1) == '/') {
                        sb.append(advance())
                        sb.append(advance())
                        break
                    }
                    sb.append(advance())
                }
                return Token(TokenType.COMMENT, sb.toString(), start, index, startLine, startCol)
            }
        }

        // 3. String literals (normal, interpolated, verbatim)
        if (c == '$' && (peek(1) == '"' || (peek(1) == '@' && peek(2) == '"'))) {
            // Interpolated string $"..."
            val sb = java.lang.StringBuilder()
            sb.append(advance()) // $
            if (peek() == '@') sb.append(advance())
            sb.append(advance()) // "
            var escaped = false
            while (index < source.length) {
                val ch = advance()
                sb.append(ch)
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    break
                }
            }
            return Token(TokenType.STRING_LITERAL, sb.toString(), start, index, startLine, startCol)
        }

        if (c == '@' && peek(1) == '"') {
            // Verbatim string @"..."
            val sb = java.lang.StringBuilder()
            sb.append(advance()) // @
            sb.append(advance()) // "
            while (index < source.length) {
                val ch = advance()
                sb.append(ch)
                if (ch == '"') {
                    if (peek() == '"') {
                        sb.append(advance()) // escaped ""
                    } else {
                        break
                    }
                }
            }
            return Token(TokenType.STRING_LITERAL, sb.toString(), start, index, startLine, startCol)
        }

        if (c == '"') {
            val sb = java.lang.StringBuilder()
            sb.append(advance()) // "
            var escaped = false
            while (index < source.length) {
                val ch = advance()
                sb.append(ch)
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    break
                }
            }
            return Token(TokenType.STRING_LITERAL, sb.toString(), start, index, startLine, startCol)
        }

        // 4. Char literal
        if (c == '\'') {
            val sb = java.lang.StringBuilder()
            sb.append(advance()) // '
            var escaped = false
            while (index < source.length) {
                val ch = advance()
                sb.append(ch)
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '\'') {
                    break
                }
            }
            return Token(TokenType.CHAR_LITERAL, sb.toString(), start, index, startLine, startCol)
        }

        // 5. Numbers
        if (c.isDigit() || (c == '.' && peek(1)?.isDigit() == true)) {
            val sb = java.lang.StringBuilder()
            var hasDot = false
            var isHex = false

            if (c == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
                sb.append(advance()) // 0
                sb.append(advance()) // x
                isHex = true
                while (index < source.length && (source[index].isDigit() || source[index] in 'a'..'f' || source[index] in 'A'..'F')) {
                    sb.append(advance())
                }
            } else {
                while (index < source.length) {
                    val ch = source[index]
                    if (ch.isDigit()) {
                        sb.append(advance())
                    } else if (ch == '.' && !hasDot && peek(1)?.isDigit() == true) {
                        hasDot = true
                        sb.append(advance())
                    } else if (ch in "fFdDmMlLuU" && sb.isNotEmpty()) {
                        sb.append(advance())
                        break
                    } else {
                        break
                    }
                }
            }
            return Token(TokenType.NUMBER_LITERAL, sb.toString(), start, index, startLine, startCol)
        }

        // 6. Identifiers and keywords
        if (c.isLetter() || c == '_') {
            val sb = java.lang.StringBuilder()
            while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) {
                sb.append(advance())
            }
            val text = sb.toString()
            val type = when {
                text == "true" || text == "false" -> TokenType.BOOLEAN_LITERAL
                text == "null" -> TokenType.NULL_LITERAL
                TYPE_KEYWORDS.contains(text) -> TokenType.TYPE_KEYWORD
                KEYWORDS.contains(text) -> TokenType.KEYWORD
                KNOWN_TYPES.contains(text) || (text.isNotEmpty() && text[0].isUpperCase()) -> TokenType.TYPE_IDENTIFIER
                KNOWN_METHODS.contains(text) -> TokenType.METHOD_IDENTIFIER
                else -> TokenType.IDENTIFIER
            }
            return Token(type, text, start, index, startLine, startCol)
        }

        // 7. Operators & Punctuation
        val multiOps = listOf("==", "!=", "<=", ">=", "++", "--", "+=", "-=", "*=", "/=", "%=", "&&", "||", "=>", "??", "?.")
        for (op in multiOps) {
            if (source.startsWith(op, index)) {
                for (i in op.indices) advance()
                return Token(TokenType.OPERATOR, op, start, index, startLine, startCol)
            }
        }

        if (c in "+-*/%=<>!&|^~?") {
            val op = advance().toString()
            return Token(TokenType.OPERATOR, op, start, index, startLine, startCol)
        }

        if (c in "{}()[];:.,") {
            val p = advance().toString()
            return Token(TokenType.PUNCTUATION, p, start, index, startLine, startCol)
        }

        // 8. Unknown fallback
        val unknown = advance().toString()
        return Token(TokenType.UNKNOWN, unknown, start, index, startLine, startCol)
    }
}
