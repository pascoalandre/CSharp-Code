package com.example.csharp.engine

sealed class AstNode {
    abstract val line: Int
}

data class Param(val type: String, val name: String)

sealed class Statement : AstNode() {
    data class Block(val statements: List<Statement>, override val line: Int) : Statement()
    data class VarDecl(val typeName: String, val name: String, val init: Expr?, override val line: Int) : Statement()
    data class Assignment(val target: Expr, val op: String, val value: Expr, override val line: Int) : Statement()
    data class ExpressionStmt(val expr: Expr, override val line: Int) : Statement()
    data class If(val condition: Expr, val thenBranch: Statement, val elseBranch: Statement?, override val line: Int) : Statement()
    data class While(val condition: Expr, val body: Statement, override val line: Int) : Statement()
    data class DoWhile(val body: Statement, val condition: Expr, override val line: Int) : Statement()
    data class For(val init: Statement?, val condition: Expr?, val update: Expr?, val body: Statement, override val line: Int) : Statement()
    data class ForEach(val typeName: String, val varName: String, val iterable: Expr, val body: Statement, override val line: Int) : Statement()
    data class Return(val value: Expr?, override val line: Int) : Statement()
    data class Break(override val line: Int) : Statement()
    data class Continue(override val line: Int) : Statement()
    data class FunctionDecl(val returnType: String, val name: String, val params: List<Param>, val body: Block, override val line: Int) : Statement()
    data class ClassDecl(val name: String, val fields: List<VarDecl>, val methods: List<FunctionDecl>, override val line: Int) : Statement()
    data class Empty(override val line: Int) : Statement()
}

sealed class InterpolatedPart {
    data class Text(val text: String) : InterpolatedPart()
    data class Expression(val expr: Expr) : InterpolatedPart()
}

sealed class Expr : AstNode() {
    data class Literal(val value: Any?, val typeName: String, override val line: Int) : Expr()
    data class Identifier(val name: String, override val line: Int) : Expr()
    data class Binary(val left: Expr, val op: String, val right: Expr, override val line: Int) : Expr()
    data class Unary(val op: String, val operand: Expr, val isPrefix: Boolean = true, override val line: Int) : Expr()
    data class Call(val callee: Expr, val args: List<Expr>, override val line: Int) : Expr()
    data class MemberAccess(val target: Expr, val member: String, override val line: Int) : Expr()
    data class Index(val target: Expr, val index: Expr, override val line: Int) : Expr()
    data class New(val typeName: String, val args: List<Expr>, val initializer: List<Expr>? = null, override val line: Int) : Expr()
    data class InterpolatedString(val parts: List<InterpolatedPart>, override val line: Int) : Expr()
    data class Ternary(val condition: Expr, val trueExpr: Expr, val falseExpr: Expr, override val line: Int) : Expr()
}

data class CSharpProgram(
    val classes: List<Statement.ClassDecl>,
    val functions: List<Statement.FunctionDecl>,
    val topLevelStatements: List<Statement>
)
