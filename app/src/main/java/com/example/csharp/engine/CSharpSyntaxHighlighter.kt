package com.example.csharp.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

object CSharpSyntaxColors {
    val Keyword = Color(0xFFC586C0)          // Soft magenta / purple
    val TypeKeyword = Color(0xFF569CD6)      // VS Code Blue
    val TypeIdentifier = Color(0xFF4EC9B0)   // VS Code Teal
    val Method = Color(0xFFDCDCAA)           // VS Code Light Yellow
    val String = Color(0xFFCE9178)           // VS Code Warm Orange
    val Char = Color(0xFFD7BA7D)             // Light gold
    val Number = Color(0xFFB5CEA8)           // Light mint green
    val Boolean = Color(0xFF569CD6)          // Keyword blue
    val Null = Color(0xFF569CD6)             // Keyword blue
    val Comment = Color(0xFF6A9955)          // Classic VS Code Green
    val Operator = Color(0xFFD4D4D4)         // Crisp light gray
    val Punctuation = Color(0xFFCCCCCC)      // Off-white
    val Identifier = Color(0xFF9CDCFE)       // Variable sky blue
    val Default = Color(0xFFD4D4D4)          // Default text
}

class CSharpSyntaxHighlighter {
    fun highlight(source: String): AnnotatedString {
        if (source.isEmpty()) return AnnotatedString("")
        
        val lexer = CSharpLexer(source)
        val tokens = lexer.tokenize()

        return buildAnnotatedString {
            append(source)

            for (token in tokens) {
                if (token.start >= source.length || token.end > source.length) continue

                val style = when (token.type) {
                    TokenType.KEYWORD -> SpanStyle(
                        color = CSharpSyntaxColors.Keyword,
                        fontWeight = FontWeight.Medium
                    )
                    TokenType.TYPE_KEYWORD -> SpanStyle(
                        color = CSharpSyntaxColors.TypeKeyword,
                        fontWeight = FontWeight.Medium
                    )
                    TokenType.TYPE_IDENTIFIER -> SpanStyle(
                        color = CSharpSyntaxColors.TypeIdentifier,
                        fontWeight = FontWeight.Normal
                    )
                    TokenType.METHOD_IDENTIFIER -> SpanStyle(
                        color = CSharpSyntaxColors.Method,
                        fontWeight = FontWeight.Normal
                    )
                    TokenType.STRING_LITERAL -> SpanStyle(
                        color = CSharpSyntaxColors.String
                    )
                    TokenType.CHAR_LITERAL -> SpanStyle(
                        color = CSharpSyntaxColors.Char
                    )
                    TokenType.NUMBER_LITERAL -> SpanStyle(
                        color = CSharpSyntaxColors.Number
                    )
                    TokenType.BOOLEAN_LITERAL -> SpanStyle(
                        color = CSharpSyntaxColors.Boolean,
                        fontWeight = FontWeight.Medium
                    )
                    TokenType.NULL_LITERAL -> SpanStyle(
                        color = CSharpSyntaxColors.Null,
                        fontWeight = FontWeight.Medium
                    )
                    TokenType.COMMENT -> SpanStyle(
                        color = CSharpSyntaxColors.Comment,
                        fontStyle = FontStyle.Italic
                    )
                    TokenType.OPERATOR -> SpanStyle(
                        color = CSharpSyntaxColors.Operator
                    )
                    TokenType.PUNCTUATION -> SpanStyle(
                        color = CSharpSyntaxColors.Punctuation
                    )
                    TokenType.IDENTIFIER -> SpanStyle(
                        color = CSharpSyntaxColors.Identifier
                    )
                    TokenType.WHITESPACE, TokenType.UNKNOWN, TokenType.EOF -> null
                }

                if (style != null && token.start < token.end) {
                    addStyle(style, token.start, token.end)
                }
            }
        }
    }
}
