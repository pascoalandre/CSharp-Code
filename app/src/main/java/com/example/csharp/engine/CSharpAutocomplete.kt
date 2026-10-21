package com.example.csharp.engine

import com.example.csharp.model.CodeSuggestion
import com.example.csharp.model.SuggestionKind

object CSharpAutocomplete {

    private val SNIPPETS = listOf(
        CodeSuggestion(
            label = "cw",
            insertText = "Console.WriteLine();",
            detail = "Console.WriteLine() snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "Console.WriteLine(".length
        ),
        CodeSuggestion(
            label = "cr",
            insertText = "Console.ReadLine();",
            detail = "Console.ReadLine() snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "Console.ReadLine();".length
        ),
        CodeSuggestion(
            label = "for",
            insertText = "for (int i = 0; i < 10; i++)\n{\n    \n}",
            detail = "for loop snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "for (int i = 0; i < 10; i++)\n{\n    ".length
        ),
        CodeSuggestion(
            label = "foreach",
            insertText = "foreach (var item in collection)\n{\n    \n}",
            detail = "foreach loop snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "foreach (var item in collection)\n{\n    ".length
        ),
        CodeSuggestion(
            label = "main",
            insertText = "static void Main(string[] args)\n{\n    \n}",
            detail = "Main method snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "static void Main(string[] args)\n{\n    ".length
        ),
        CodeSuggestion(
            label = "class",
            insertText = "class MyClass\n{\n    \n}",
            detail = "class declaration snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "class MyClass\n{\n    ".length
        ),
        CodeSuggestion(
            label = "if",
            insertText = "if (true)\n{\n    \n}",
            detail = "if statement snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "if (true)\n{\n    ".length
        ),
        CodeSuggestion(
            label = "ifelse",
            insertText = "if (true)\n{\n    \n}\nelse\n{\n    \n}",
            detail = "if-else block snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "if (true)\n{\n    ".length
        ),
        CodeSuggestion(
            label = "while",
            insertText = "while (condition)\n{\n    \n}",
            detail = "while loop snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "while (condition)\n{\n    ".length
        ),
        CodeSuggestion(
            label = "trycatch",
            insertText = "try\n{\n    \n}\ncatch (Exception ex)\n{\n    Console.WriteLine(ex.Message);\n}",
            detail = "try-catch block snippet",
            kind = SuggestionKind.SNIPPET,
            cursorOffset = "try\n{\n    ".length
        )
    )

    private val BUILTIN_APIS = listOf(
        CodeSuggestion("Console.WriteLine", "Console.WriteLine();", "void Console.WriteLine(object value)", SuggestionKind.METHOD, "Console.WriteLine(".length),
        CodeSuggestion("Console.Write", "Console.Write();", "void Console.Write(object value)", SuggestionKind.METHOD, "Console.Write(".length),
        CodeSuggestion("Console.ReadLine", "Console.ReadLine()", "string Console.ReadLine()", SuggestionKind.METHOD),
        CodeSuggestion("Console.Clear", "Console.Clear();", "void Console.Clear()", SuggestionKind.METHOD),
        CodeSuggestion("Math.Sqrt", "Math.Sqrt()", "double Math.Sqrt(double d)", SuggestionKind.METHOD, "Math.Sqrt(".length),
        CodeSuggestion("Math.Pow", "Math.Pow(, )", "double Math.Pow(double x, double y)", SuggestionKind.METHOD, "Math.Pow(".length),
        CodeSuggestion("Math.Max", "Math.Max(, )", "T Math.Max(val1, val2)", SuggestionKind.METHOD, "Math.Max(".length),
        CodeSuggestion("Math.Min", "Math.Min(, )", "T Math.Min(val1, val2)", SuggestionKind.METHOD, "Math.Min(".length),
        CodeSuggestion("Math.Abs", "Math.Abs()", "T Math.Abs(value)", SuggestionKind.METHOD, "Math.Abs(".length),
        CodeSuggestion("Math.PI", "Math.PI", "const double Math.PI = 3.14159...", SuggestionKind.PROPERTY),
        CodeSuggestion("DateTime.Now", "DateTime.Now", "DateTime DateTime.Now", SuggestionKind.PROPERTY),
        CodeSuggestion("Convert.ToInt32", "Convert.ToInt32()", "int Convert.ToInt32(value)", SuggestionKind.METHOD, "Convert.ToInt32(".length),
        CodeSuggestion("int.Parse", "int.Parse()", "int int.Parse(string s)", SuggestionKind.METHOD, "int.Parse(".length),
        CodeSuggestion("List<int>", "List<int>()", "class System.Collections.Generic.List<T>", SuggestionKind.CLASS),
        CodeSuggestion("Dictionary<string, int>", "Dictionary<string, int>()", "class Dictionary<TKey, TValue>", SuggestionKind.CLASS)
    )

    private val KEYWORD_SUGGESTIONS = listOf(
        "using", "namespace", "class", "public", "private", "protected", "static", "void",
        "int", "string", "bool", "double", "float", "var", "new", "return", "if", "else",
        "for", "foreach", "while", "break", "continue", "switch", "case", "default",
        "try", "catch", "finally", "throw", "true", "false", "null", "this"
    ).map {
        CodeSuggestion(it, it, "keyword $it", SuggestionKind.KEYWORD)
    }

    /**
     * Get suggestions based on text and cursor position.
     */
    fun getSuggestions(source: String, cursorIndex: Int): List<CodeSuggestion> {
        val safeCursor = cursorIndex.coerceIn(0, source.length)
        val prefix = getPrefixAt(source, safeCursor)
        if (prefix.isEmpty()) {
            return emptyList()
        }

        val dynamicIdentifiers = extractIdentifiers(source)
            .filter { it != prefix && it.startsWith(prefix, ignoreCase = true) }
            .map {
                CodeSuggestion(
                    label = it,
                    insertText = it,
                    detail = "local identifier",
                    kind = SuggestionKind.VARIABLE
                )
            }

        val allCandidates = SNIPPETS + BUILTIN_APIS + KEYWORD_SUGGESTIONS + dynamicIdentifiers

        return allCandidates
            .filter { candidate ->
                candidate.label.startsWith(prefix, ignoreCase = true) ||
                (candidate.label.contains(".") && candidate.label.substringAfter(".").startsWith(prefix, ignoreCase = true))
            }
            .distinctBy { it.label }
            .take(8)
    }

    private fun getPrefixAt(source: String, cursor: Int): String {
        var start = cursor
        while (start > 0) {
            val c = source[start - 1]
            if (c.isLetterOrDigit() || c == '_' || c == '.') {
                start--
            } else {
                break
            }
        }
        return source.substring(start, cursor)
    }

    private fun extractIdentifiers(source: String): Set<String> {
        val regex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        return regex.findAll(source).map { it.value }.toSet()
    }
}
