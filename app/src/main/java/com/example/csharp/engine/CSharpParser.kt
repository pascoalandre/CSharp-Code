package com.example.csharp.engine

class CSharpParser(private val source: String) {
    private val tokens: List<Token>
    private var current = 0

    init {
        val lexer = CSharpLexer(source)
        // Filter out whitespace and comments for parsing
        tokens = lexer.tokenize().filter {
            it.type != TokenType.WHITESPACE && it.type != TokenType.COMMENT
        }
    }

    private fun isAtEnd(): Boolean = current >= tokens.size

    private fun peek(offset: Int = 0): Token {
        val pos = current + offset
        return if (pos < tokens.size) tokens[pos] else Token(TokenType.EOF, "", source.length, source.length, if (tokens.isNotEmpty()) tokens.last().line else 1, 1)
    }

    private fun previous(): Token = tokens[current - 1]

    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun check(text: String): Boolean {
        if (isAtEnd()) return false
        return peek().text == text
    }

    private fun checkType(type: TokenType): Boolean {
        if (isAtEnd()) return false
        return peek().type == type
    }

    private fun match(vararg texts: String): Boolean {
        for (text in texts) {
            if (check(text)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(expected: String, message: String): Token {
        if (check(expected)) return advance()
        val found = if (isAtEnd()) "EOF" else "'${peek().text}'"
        val line = if (isAtEnd()) (if (tokens.isNotEmpty()) tokens.last().line else 1) else peek().line
        throw ParseException("Line $line: $message (found $found)")
    }

    class ParseException(message: String) : RuntimeException(message)

    fun parseProgram(): CSharpProgram {
        val classes = mutableListOf<Statement.ClassDecl>()
        val functions = mutableListOf<Statement.FunctionDecl>()
        val topLevelStmts = mutableListOf<Statement>()

        while (!isAtEnd()) {
            // Ignore using directives: using System; using System.Collections.Generic;
            if (check("using")) {
                advance()
                while (!isAtEnd() && !check(";")) {
                    advance()
                }
                if (check(";")) advance()
                continue
            }

            // Ignore namespace wrapper: namespace MyApp { ... }
            if (check("namespace")) {
                advance()
                consumeNamespaceOrName()
                if (check("{")) {
                    advance()
                    continue
                }
            }

            if (check("}")) {
                advance()
                continue
            }

            // Check for class declaration
            val isClass = isClassDeclaration()
            if (isClass) {
                classes.add(parseClassDeclaration())
                continue
            }

            // Check for method declaration
            val isMethod = isMethodDeclaration()
            if (isMethod) {
                functions.add(parseMethodDeclaration())
                continue
            }

            // Otherwise, statement
            topLevelStmts.add(parseStatement())
        }

        return CSharpProgram(classes, functions, topLevelStmts)
    }

    private fun consumeNamespaceOrName() {
        while (!isAtEnd() && !check("{") && !check(";")) {
            advance()
        }
    }

    private fun isClassDeclaration(): Boolean {
        var idx = 0
        while (current + idx < tokens.size) {
            val t = peek(idx).text
            if (t in listOf("public", "private", "protected", "internal", "static", "sealed", "abstract", "partial")) {
                idx++
                continue
            }
            return t == "class" || t == "struct" || t == "record"
        }
        return false
    }

    private fun parseClassDeclaration(): Statement.ClassDecl {
        val startLine = peek().line
        // Skip modifiers
        while (!isAtEnd() && peek().text in listOf("public", "private", "protected", "internal", "static", "sealed", "abstract", "partial")) {
            advance()
        }
        advance() // "class"
        val className = consumeIdentifier("Expected class name")
        
        // Skip inheritance if any : BaseClass, IFoo
        if (check(":")) {
            advance()
            while (!isAtEnd() && !check("{")) advance()
        }

        consume("{", "Expected '{' to start class body")

        val fields = mutableListOf<Statement.VarDecl>()
        val methods = mutableListOf<Statement.FunctionDecl>()

        while (!isAtEnd() && !check("}")) {
            if (isMethodDeclaration()) {
                methods.add(parseMethodDeclaration())
            } else {
                val stmt = parseStatement()
                if (stmt is Statement.VarDecl) {
                    fields.add(stmt)
                }
            }
        }

        consume("}", "Expected '}' at end of class body")
        return Statement.ClassDecl(className, fields, methods, startLine)
    }

    private fun isMethodDeclaration(): Boolean {
        var idx = 0
        while (current + idx < tokens.size) {
            val t = peek(idx).text
            if (t in listOf("public", "private", "protected", "internal", "static", "async", "virtual", "override", "new")) {
                idx++
                continue
            }
            break
        }
        // Need type, identifier, and '('
        val typeIdx = current + idx
        val nameIdx = typeIdx + 1
        val parenIdx = nameIdx + 1
        if (parenIdx < tokens.size && tokens[parenIdx].text == "(") {
            // Verify name is an identifier and not a keyword like 'if', 'while', 'for', 'switch', 'catch'
            val nameText = tokens[nameIdx].text
            if (nameText !in listOf("if", "while", "for", "foreach", "switch", "catch", "using", "lock")) {
                return true
            }
        }
        return false
    }

    private fun parseMethodDeclaration(): Statement.FunctionDecl {
        val line = peek().line
        while (!isAtEnd() && peek().text in listOf("public", "private", "protected", "internal", "static", "async", "virtual", "override", "new")) {
            advance()
        }
        val returnType = parseType()
        val name = consumeIdentifier("Expected method name")

        consume("(", "Expected '(' after method name")
        val params = mutableListOf<Param>()
        if (!check(")")) {
            do {
                val pType = parseType()
                val pName = consumeIdentifier("Expected parameter name")
                params.add(Param(pType, pName))
            } while (match(","))
        }
        consume(")", "Expected ')' after parameters")

        // Method body { ... } or expression body => expr;
        val body = if (match("=>")) {
            val exprLine = peek().line
            val expr = parseExpression()
            consume(";", "Expected ';' after expression body")
            Statement.Block(listOf(Statement.Return(expr, exprLine)), exprLine)
        } else {
            parseBlock()
        }

        return Statement.FunctionDecl(returnType, name, params, body, line)
    }

    private fun parseType(): String {
        val sb = StringBuilder()
        sb.append(advance().text)
        // Handle generics List<int>
        if (check("<")) {
            sb.append(advance().text) // <
            while (!isAtEnd() && !check(">")) {
                sb.append(advance().text)
            }
            if (check(">")) sb.append(advance().text)
        }
        // Handle array brackets []
        while (check("[") && peek(1).text == "]") {
            sb.append("[]")
            advance()
            advance()
        }
        // Handle nullable ?
        if (check("?")) {
            sb.append(advance().text)
        }
        return sb.toString()
    }

    private fun consumeIdentifier(errorMsg: String): String {
        if (checkType(TokenType.IDENTIFIER) || checkType(TokenType.TYPE_IDENTIFIER) || checkType(TokenType.METHOD_IDENTIFIER)) {
            return advance().text
        }
        // Fallback for contextual keywords
        if (!isAtEnd() && peek().text !in listOf(";", "{", "}", "(", ")", "[", "]")) {
            return advance().text
        }
        throw ParseException("Line ${peek().line}: $errorMsg (found '${peek().text}')")
    }

    private fun parseStatement(): Statement {
        val line = peek().line

        if (match(";")) return Statement.Empty(line)
        if (check("{")) return parseBlock()
        if (match("if")) return parseIfStatement(line)
        if (match("while")) return parseWhileStatement(line)
        if (match("do")) return parseDoWhileStatement(line)
        if (match("for")) return parseForStatement(line)
        if (match("foreach")) return parseForEachStatement(line)
        if (match("return")) return parseReturnStatement(line)
        if (match("break")) {
            consume(";", "Expected ';' after break")
            return Statement.Break(line)
        }
        if (match("continue")) {
            consume(";", "Expected ';' after continue")
            return Statement.Continue(line)
        }

        // Variable declaration check
        if (isVarDecl()) {
            return parseVarDeclStatement(line)
        }

        // Otherwise Expression statement (e.g. function call, assignment, increment)
        val expr = parseExpression()
        if (match("=")) {
            val value = parseExpression()
            consume(";", "Expected ';' after assignment")
            return Statement.Assignment(expr, "=", value, line)
        }
        for (op in listOf("+=", "-=", "*=", "/=", "%=")) {
            if (match(op)) {
                val value = parseExpression()
                consume(";", "Expected ';' after assignment")
                return Statement.Assignment(expr, op, value, line)
            }
        }

        consume(";", "Expected ';' after statement")
        return Statement.ExpressionStmt(expr, line)
    }

    private fun parseBlock(): Statement.Block {
        val line = peek().line
        consume("{", "Expected '{'")
        val stmts = mutableListOf<Statement>()
        while (!isAtEnd() && !check("}")) {
            stmts.add(parseStatement())
        }
        consume("}", "Expected '}'")
        return Statement.Block(stmts, line)
    }

    private fun parseIfStatement(line: Int): Statement.If {
        consume("(", "Expected '(' after if")
        val condition = parseExpression()
        consume(")", "Expected ')' after if condition")
        val thenBranch = parseStatement()
        val elseBranch = if (match("else")) parseStatement() else null
        return Statement.If(condition, thenBranch, elseBranch, line)
    }

    private fun parseWhileStatement(line: Int): Statement.While {
        consume("(", "Expected '(' after while")
        val condition = parseExpression()
        consume(")", "Expected ')' after while condition")
        val body = parseStatement()
        return Statement.While(condition, body, line)
    }

    private fun parseDoWhileStatement(line: Int): Statement.DoWhile {
        val body = parseStatement()
        consume("while", "Expected 'while' after do body")
        consume("(", "Expected '(' after while")
        val condition = parseExpression()
        consume(")", "Expected ')' after condition")
        consume(";", "Expected ';' after do-while")
        return Statement.DoWhile(body, condition, line)
    }

    private fun parseForStatement(line: Int): Statement.For {
        consume("(", "Expected '(' after for")
        val init = if (match(";")) null else {
            if (isVarDecl()) parseVarDeclStatement(line) else {
                val e = parseExpression()
                consume(";", "Expected ';' in for init")
                Statement.ExpressionStmt(e, line)
            }
        }
        val condition = if (check(";")) null else parseExpression()
        consume(";", "Expected ';' in for condition")
        val update = if (check(")")) null else parseExpression()
        consume(")", "Expected ')' after for clauses")
        val body = parseStatement()
        return Statement.For(init, condition, update, body, line)
    }

    private fun parseForEachStatement(line: Int): Statement.ForEach {
        consume("(", "Expected '(' after foreach")
        val type = parseType()
        val name = consumeIdentifier("Expected variable name in foreach")
        consume("in", "Expected 'in' in foreach loop")
        val iterable = parseExpression()
        consume(")", "Expected ')' after foreach")
        val body = parseStatement()
        return Statement.ForEach(type, name, iterable, body, line)
    }

    private fun parseReturnStatement(line: Int): Statement.Return {
        val value = if (check(";")) null else parseExpression()
        consume(";", "Expected ';' after return")
        return Statement.Return(value, line)
    }

    private fun isVarDecl(): Boolean {
        // e.g. int x = 10; var x = 10; string s; List<int> list;
        val first = peek().text
        if (first == "var" || first in CSharpLexer.TYPE_KEYWORDS) return true
        if (peek().type == TokenType.TYPE_IDENTIFIER) {
            // Check next token is identifier or '<' or '[]'
            val next = peek(1).text
            if (next == "<" || next == "[" || peek(1).type == TokenType.IDENTIFIER) return true
        }
        return false
    }

    private fun parseVarDeclStatement(line: Int): Statement.VarDecl {
        val type = parseType()
        val name = consumeIdentifier("Expected variable name")
        val init = if (match("=")) {
            if (check("{")) {
                // Array initializer: int[] a = { 1, 2, 3 };
                parseArrayLiteral(line)
            } else {
                parseExpression()
            }
        } else null
        consume(";", "Expected ';' after variable declaration")
        return Statement.VarDecl(type, name, init, line)
    }

    private fun parseArrayLiteral(line: Int): Expr {
        consume("{", "Expected '{'")
        val items = mutableListOf<Expr>()
        if (!check("}")) {
            do {
                items.add(parseExpression())
            } while (match(","))
        }
        consume("}", "Expected '}'")
        return Expr.New("Array", emptyList(), items, line)
    }

    // Expressions parser with Pratt / Precedence Climbing
    fun parseExpression(): Expr = parseTernary()

    private fun parseTernary(): Expr {
        var expr = parseLogicalOr()
        if (match("?")) {
            val trueExpr = parseExpression()
            consume(":", "Expected ':' in ternary expression")
            val falseExpr = parseExpression()
            expr = Expr.Ternary(expr, trueExpr, falseExpr, expr.line)
        }
        return expr
    }

    private fun parseLogicalOr(): Expr {
        var expr = parseLogicalAnd()
        while (match("||")) {
            val op = previous().text
            val right = parseLogicalAnd()
            expr = Expr.Binary(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseLogicalAnd(): Expr {
        var expr = parseEquality()
        while (match("&&")) {
            val op = previous().text
            val right = parseEquality()
            expr = Expr.Binary(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseEquality(): Expr {
        var expr = parseRelational()
        while (match("==", "!=")) {
            val op = previous().text
            val right = parseRelational()
            expr = Expr.Binary(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseRelational(): Expr {
        var expr = parseAdditive()
        while (match("<", "<=", ">", ">=")) {
            val op = previous().text
            val right = parseAdditive()
            expr = Expr.Binary(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseAdditive(): Expr {
        var expr = parseMultiplicative()
        while (match("+", "-")) {
            val op = previous().text
            val right = parseMultiplicative()
            expr = Expr.Binary(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseMultiplicative(): Expr {
        var expr = parseUnary()
        while (match("*", "/", "%")) {
            val op = previous().text
            val right = parseUnary()
            expr = Expr.Binary(expr, op, right, expr.line)
        }
        return expr
    }

    private fun parseUnary(): Expr {
        if (match("!", "-", "+", "++", "--")) {
            val op = previous().text
            val line = previous().line
            val right = parseUnary()
            return Expr.Unary(op, right, isPrefix = true, line = line)
        }
        return parsePostfix()
    }

    private fun parsePostfix(): Expr {
        var expr = parsePrimary()
        while (true) {
            when {
                match("++", "--") -> {
                    val op = previous().text
                    expr = Expr.Unary(op, expr, isPrefix = false, line = expr.line)
                }
                match("(") -> {
                    val args = mutableListOf<Expr>()
                    if (!check(")")) {
                        do {
                            args.add(parseExpression())
                        } while (match(","))
                    }
                    consume(")", "Expected ')' after call arguments")
                    expr = Expr.Call(expr, args, expr.line)
                }
                match(".") -> {
                    val member = consumeIdentifier("Expected member name after '.'")
                    expr = Expr.MemberAccess(expr, member, expr.line)
                }
                match("[") -> {
                    val index = parseExpression()
                    consume("]", "Expected ']' after index")
                    expr = Expr.Index(expr, index, expr.line)
                }
                else -> break
            }
        }
        return expr
    }

    private fun parsePrimary(): Expr {
        val token = peek()
        val line = token.line

        if (match("true")) return Expr.Literal(true, "bool", line)
        if (match("false")) return Expr.Literal(false, "bool", line)
        if (match("null")) return Expr.Literal(null, "null", line)

        if (match("new")) {
            val typeName = parseType()
            var args = emptyList<Expr>()
            if (match("(")) {
                val argList = mutableListOf<Expr>()
                if (!check(")")) {
                    do {
                        argList.add(parseExpression())
                    } while (match(","))
                }
                consume(")", "Expected ')' after new constructor args")
                args = argList
            } else if (match("[")) {
                val sizeExpr = if (check("]")) null else parseExpression()
                consume("]", "Expected ']'")
                if (sizeExpr != null) {
                    args = listOf(sizeExpr)
                }
            }

            var initializer: List<Expr>? = null
            if (check("{")) {
                consume("{", "Expected '{'")
                val initList = mutableListOf<Expr>()
                if (!check("}")) {
                    do {
                        initList.add(parseExpression())
                    } while (match(","))
                }
                consume("}", "Expected '}'")
                initializer = initList
            }
            return Expr.New(typeName, args, initializer, line)
        }

        if (checkType(TokenType.NUMBER_LITERAL)) {
            val text = advance().text
            val clean = text.trimEnd('f', 'F', 'd', 'D', 'm', 'M', 'l', 'L', 'u', 'U')
            return if (clean.contains(".") || text.endsWith("f", true) || text.endsWith("d", true)) {
                Expr.Literal(clean.toDoubleOrNull() ?: 0.0, "double", line)
            } else {
                Expr.Literal(clean.toLongOrNull() ?: 0L, "int", line)
            }
        }

        if (checkType(TokenType.CHAR_LITERAL)) {
            val text = advance().text
            val ch = if (text.length >= 3) text[1] else ' '
            return Expr.Literal(ch, "char", line)
        }

        if (checkType(TokenType.STRING_LITERAL)) {
            val raw = advance().text
            if (raw.startsWith("$\"")) {
                // Interpolated string!
                return parseInterpolatedString(raw, line)
            } else {
                val content = unescapeString(raw)
                return Expr.Literal(content, "string", line)
            }
        }

        if (match("(")) {
            val expr = parseExpression()
            consume(")", "Expected ')'")
            return expr
        }

        if (checkType(TokenType.IDENTIFIER) || checkType(TokenType.TYPE_IDENTIFIER) || checkType(TokenType.METHOD_IDENTIFIER)) {
            val name = advance().text
            return Expr.Identifier(name, line)
        }

        throw ParseException("Line $line: Unexpected token '${token.text}'")
    }

    private fun parseInterpolatedString(raw: String, line: Int): Expr {
        // e.g. $"Hello {name}, count is {list.Count + 1}!"
        val parts = mutableListOf<InterpolatedPart>()
        val content = raw.removePrefix("$\"").removeSuffix("\"")
        var i = 0
        val textBuf = StringBuilder()

        while (i < content.length) {
            val c = content[i]
            if (c == '{') {
                if (i + 1 < content.length && content[i + 1] == '{') {
                    // Escaped {{
                    textBuf.append('{')
                    i += 2
                    continue
                }
                if (textBuf.isNotEmpty()) {
                    parts.add(InterpolatedPart.Text(textBuf.toString()))
                    textBuf.clear()
                }
                i++ // skip {
                val exprBuf = StringBuilder()
                var braceCount = 1
                while (i < content.length && braceCount > 0) {
                    val ch = content[i]
                    if (ch == '{') braceCount++
                    else if (ch == '}') braceCount--
                    if (braceCount > 0) exprBuf.append(ch)
                    i++
                }
                val subExpr = try {
                    val subParser = CSharpParser(exprBuf.toString())
                    subParser.parseExpression()
                } catch (e: Exception) {
                    Expr.Literal("{$exprBuf}", "string", line)
                }
                parts.add(InterpolatedPart.Expression(subExpr))
            } else {
                textBuf.append(c)
                i++
            }
        }
        if (textBuf.isNotEmpty()) {
            parts.add(InterpolatedPart.Text(textBuf.toString()))
        }
        return Expr.InterpolatedString(parts, line)
    }

    private fun unescapeString(raw: String): String {
        val s = if (raw.startsWith("@\"")) {
            raw.removePrefix("@\"").removeSuffix("\"").replace("\"\"", "\"")
        } else {
            raw.removePrefix("\"").removeSuffix("\"")
        }
        return s.replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
