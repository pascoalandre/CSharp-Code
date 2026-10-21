package com.example.csharp.engine

import com.example.csharp.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class ReturnException(val value: Any?) : RuntimeException()
class BreakException : RuntimeException()
class ContinueException : RuntimeException()

class CSharpInstance(
    val className: String,
    val fields: MutableMap<String, Any?> = mutableMapOf()
) {
    override fun toString(): String = "$className { ${fields.map { "${it.key}: ${it.value}" }.joinToString(", ")} }"
}

class Scope(val parent: Scope? = null) {
    val variables = mutableMapOf<String, Pair<String, Any?>>() // name -> (type, value)

    fun define(name: String, type: String, value: Any?) {
        variables[name] = Pair(type, value)
    }

    fun assign(name: String, value: Any?): Boolean {
        if (variables.containsKey(name)) {
            val oldType = variables[name]!!.first
            variables[name] = Pair(oldType, value)
            return true
        }
        return parent?.assign(name, value) ?: false
    }

    fun get(name: String): Any? {
        if (variables.containsKey(name)) {
            return variables[name]!!.second
        }
        return parent?.get(name)
    }

    fun getType(name: String): String {
        if (variables.containsKey(name)) {
            return variables[name]!!.first
        }
        return parent?.getType(name) ?: "object"
    }

    fun getAllVisible(): Map<String, CSharpVariable> {
        val map = mutableMapOf<String, CSharpVariable>()
        parent?.getAllVisible()?.let { map.putAll(it) }
        variables.forEach { (k, v) ->
            map[k] = CSharpVariable(
                name = k,
                type = v.first,
                value = formatValue(v.second),
                rawValue = v.second
            )
        }
        return map
    }

    private fun formatValue(v: Any?): String {
        return when (v) {
            null -> "null"
            is String -> "\"$v\""
            is Char -> "'$v'"
            is List<*> -> "[${v.joinToString(", ")}]"
            is Array<*> -> "[${v.joinToString(", ")}]"
            is CSharpInstance -> v.toString()
            else -> v.toString()
        }
    }
}

class CSharpInterpreter(
    private val breakpointsProvider: () -> Set<Int>,
    private val onOutput: (ConsoleEntry) -> Unit,
    private val onStateChange: (DebugState, Int, List<StackFrame>) -> Unit,
    private val onConsoleClear: () -> Unit
) {
    private var isDebug = false
    private var stepMode = StepMode.NONE
    private var stepTargetFrameDepth = 0
    private var lastSteppedLine = -1

    private var currentLine = 1
    private val callStackFrames = mutableListOf<StackFrame>()
    private val functions = mutableMapOf<String, Statement.FunctionDecl>()
    private val classes = mutableMapOf<String, Statement.ClassDecl>()

    private var resumeSignal: CompletableDeferred<Unit>? = null
    private val inputChannel = Channel<String>(Channel.BUFFERED)
    private var isStopped = false

    fun sendInput(input: String) {
        inputChannel.trySend(input)
    }

    fun stepOver() {
        stepMode = StepMode.STEP_OVER
        stepTargetFrameDepth = callStackFrames.size
        resume()
    }

    fun stepInto() {
        stepMode = StepMode.STEP_INTO
        stepTargetFrameDepth = callStackFrames.size + 1
        resume()
    }

    fun stepOut() {
        stepMode = StepMode.STEP_OUT
        stepTargetFrameDepth = (callStackFrames.size - 1).coerceAtLeast(1)
        resume()
    }

    fun continueExecution() {
        stepMode = StepMode.NONE
        resume()
    }

    fun pauseExecution() {
        stepMode = StepMode.STEP_OVER
        stepTargetFrameDepth = callStackFrames.size
    }

    fun stop() {
        isStopped = true
        resumeSignal?.complete(Unit)
    }

    private fun resume() {
        val signal = resumeSignal
        resumeSignal = null
        signal?.complete(Unit)
    }

    suspend fun execute(program: CSharpProgram, debugMode: Boolean) {
        isDebug = debugMode
        isStopped = false
        stepMode = if (debugMode) StepMode.STEP_INTO else StepMode.NONE
        stepTargetFrameDepth = 1
        callStackFrames.clear()
        functions.clear()
        classes.clear()

        try {
            // Register all classes and functions
            for (c in program.classes) {
                classes[c.name] = c
                for (m in c.methods) {
                    functions[m.name] = m
                    functions["${c.name}.${m.name}"] = m
                }
            }
            for (f in program.functions) {
                functions[f.name] = f
            }

            val rootScope = Scope()

            // Find entry point:
            // 1. If there's top level statements, execute them in Main
            // 2. Else if there is a Main method in a class or function, execute Main
            val hasTopLevel = program.topLevelStatements.isNotEmpty()
            val mainFunction = functions["Main"] ?: functions.entries.firstOrNull { it.key.endsWith(".Main") }?.value

            pushFrame("Main", "Program", if (hasTopLevel) program.topLevelStatements.first().line else (mainFunction?.line ?: 1), rootScope)

            onOutput(ConsoleEntry(type = ConsoleEntryType.SYSTEM, text = if (debugMode) "--- Sessão de depuração C# iniciada ---" else "--- Executando programa C# ---"))

            if (hasTopLevel) {
                for (stmt in program.topLevelStatements) {
                    executeStatement(stmt, rootScope)
                }
            } else if (mainFunction != null) {
                for (stmt in mainFunction.body.statements) {
                    executeStatement(stmt, rootScope)
                }
            } else {
                onOutput(ConsoleEntry(type = ConsoleEntryType.STDERR, text = "Erro: Ponto de entrada não encontrado. Defina instruções de nível superior ou um método 'static void Main()'." ))
            }

            popFrame()
            onOutput(ConsoleEntry(type = ConsoleEntryType.SYSTEM, text = "--- Programa finalizado com código de saída 0 ---"))
            onStateChange(DebugState.FINISHED, 0, emptyList())

        } catch (e: CancellationException) {
            onOutput(ConsoleEntry(type = ConsoleEntryType.SYSTEM, text = "--- Execução interrompida pelo usuário ---"))
            onStateChange(DebugState.IDLE, 0, emptyList())
        } catch (e: Exception) {
            if (isStopped) {
                onOutput(ConsoleEntry(type = ConsoleEntryType.SYSTEM, text = "--- Depuração abortada ---"))
                onStateChange(DebugState.IDLE, 0, emptyList())
            } else {
                val lineInfo = if (currentLine > 0) " na linha $currentLine" else ""
                onOutput(ConsoleEntry(type = ConsoleEntryType.STDERR, text = "Exceção não tratada$lineInfo: ${e.message ?: e.javaClass.simpleName}"))
                onStateChange(DebugState.ERROR, currentLine, callStackFrames.toList())
            }
        }
    }

    private fun pushFrame(functionName: String, className: String, line: Int, scope: Scope) {
        val frame = StackFrame(
            functionName = functionName,
            className = className,
            lineNumber = line,
            variables = scope.getAllVisible()
        )
        callStackFrames.add(frame)
        currentLine = line
    }

    private fun updateTopFrameLine(line: Int, scope: Scope) {
        if (callStackFrames.isNotEmpty()) {
            val top = callStackFrames.removeAt(callStackFrames.lastIndex)
            callStackFrames.add(top.copy(lineNumber = line, variables = scope.getAllVisible()))
            currentLine = line
        }
    }

    private fun popFrame() {
        if (callStackFrames.isNotEmpty()) {
            callStackFrames.removeAt(callStackFrames.lastIndex)
            currentLine = callStackFrames.lastOrNull()?.lineNumber ?: 0
        }
    }

    private suspend fun checkBreakpointAndPause(line: Int, scope: Scope) {
        if (isStopped) throw CancellationException()
        if (line <= 0) return

        updateTopFrameLine(line, scope)

        val breakpoints = breakpointsProvider()
        val isAtBreakpoint = breakpoints.contains(line)
        val isSingleStepping = when (stepMode) {
            StepMode.STEP_INTO -> true
            StepMode.STEP_OVER -> callStackFrames.size <= stepTargetFrameDepth && line != lastSteppedLine
            StepMode.STEP_OUT -> callStackFrames.size < stepTargetFrameDepth
            StepMode.NONE -> false
        }

        if (isDebug && (isAtBreakpoint || isSingleStepping)) {
            lastSteppedLine = line
            val pauseDeferred = CompletableDeferred<Unit>()
            resumeSignal = pauseDeferred

            val reason = if (isAtBreakpoint) "Breakpoint atingido na linha $line" else "Passo do depurador na linha $line"
            onOutput(ConsoleEntry(type = ConsoleEntryType.DEBUG, text = "[Depurador] $reason"))
            onStateChange(DebugState.PAUSED, line, callStackFrames.toList())

            // Await user action
            pauseDeferred.await()

            if (isStopped) throw CancellationException()
            onStateChange(DebugState.RUNNING, line, callStackFrames.toList())
        }
    }

    private suspend fun executeStatement(stmt: Statement, scope: Scope) {
        if (isStopped) throw CancellationException()
        checkBreakpointAndPause(stmt.line, scope)

        when (stmt) {
            is Statement.Block -> {
                val blockScope = Scope(scope)
                for (s in stmt.statements) {
                    executeStatement(s, blockScope)
                }
            }
            is Statement.VarDecl -> {
                val value = if (stmt.init != null) evalExpr(stmt.init, scope) else defaultTypeValue(stmt.typeName)
                scope.define(stmt.name, stmt.typeName, value)
                updateTopFrameLine(stmt.line, scope)
            }
            is Statement.Assignment -> {
                val rightVal = evalExpr(stmt.value, scope)
                applyAssignment(stmt.target, stmt.op, rightVal, scope)
                updateTopFrameLine(stmt.line, scope)
            }
            is Statement.ExpressionStmt -> {
                evalExpr(stmt.expr, scope)
                updateTopFrameLine(stmt.line, scope)
            }
            is Statement.If -> {
                val cond = isTruthy(evalExpr(stmt.condition, scope))
                if (cond) {
                    executeStatement(stmt.thenBranch, scope)
                } else if (stmt.elseBranch != null) {
                    executeStatement(stmt.elseBranch, scope)
                }
            }
            is Statement.While -> {
                while (isTruthy(evalExpr(stmt.condition, scope))) {
                    try {
                        executeStatement(stmt.body, scope)
                    } catch (e: BreakException) {
                        break
                    } catch (e: ContinueException) {
                        continue
                    }
                }
            }
            is Statement.DoWhile -> {
                do {
                    try {
                        executeStatement(stmt.body, scope)
                    } catch (e: BreakException) {
                        break
                    } catch (e: ContinueException) {
                        // continue
                    }
                } while (isTruthy(evalExpr(stmt.condition, scope)))
            }
            is Statement.For -> {
                val forScope = Scope(scope)
                if (stmt.init != null) {
                    executeStatement(stmt.init, forScope)
                }
                while (stmt.condition == null || isTruthy(evalExpr(stmt.condition, forScope))) {
                    try {
                        executeStatement(stmt.body, forScope)
                    } catch (e: BreakException) {
                        break
                    } catch (e: ContinueException) {
                        // continue to update
                    }
                    if (stmt.update != null) {
                        evalExpr(stmt.update, forScope)
                    }
                }
            }
            is Statement.ForEach -> {
                val iterableVal = evalExpr(stmt.iterable, scope)
                val items = when (iterableVal) {
                    is List<*> -> iterableVal
                    is Array<*> -> iterableVal.toList()
                    is String -> iterableVal.map { it.toString() }
                    else -> throw RuntimeException("Objeto do tipo ${iterableVal?.javaClass?.simpleName} não é iterável no foreach.")
                }
                for (item in items) {
                    val loopScope = Scope(scope)
                    loopScope.define(stmt.varName, stmt.typeName, item)
                    try {
                        executeStatement(stmt.body, loopScope)
                    } catch (e: BreakException) {
                        break
                    } catch (e: ContinueException) {
                        continue
                    }
                }
            }
            is Statement.Return -> {
                val value = stmt.value?.let { evalExpr(it, scope) }
                throw ReturnException(value)
            }
            is Statement.Break -> throw BreakException()
            is Statement.Continue -> throw ContinueException()
            is Statement.FunctionDecl -> {
                functions[stmt.name] = stmt
            }
            is Statement.ClassDecl -> {
                classes[stmt.name] = stmt
            }
            is Statement.Empty -> { /* no-op */ }
        }
    }

    private suspend fun evalExpr(expr: Expr, scope: Scope): Any? {
        if (isStopped) throw CancellationException()

        return when (expr) {
            is Expr.Literal -> expr.value
            is Expr.Identifier -> {
                if (expr.name == "Math") "Math"
                else if (expr.name == "Console") "Console"
                else if (expr.name == "DateTime") "DateTime"
                else if (expr.name == "Convert") "Convert"
                else scope.get(expr.name)
            }
            is Expr.Binary -> evalBinary(expr, scope)
            is Expr.Unary -> evalUnary(expr, scope)
            is Expr.Ternary -> {
                val cond = isTruthy(evalExpr(expr.condition, scope))
                if (cond) evalExpr(expr.trueExpr, scope) else evalExpr(expr.falseExpr, scope)
            }
            is Expr.InterpolatedString -> {
                val sb = StringBuilder()
                for (part in expr.parts) {
                    when (part) {
                        is InterpolatedPart.Text -> sb.append(part.text)
                        is InterpolatedPart.Expression -> sb.append(evalExpr(part.expr, scope)?.toString() ?: "null")
                    }
                }
                sb.toString()
            }
            is Expr.MemberAccess -> evalMemberAccess(expr, scope)
            is Expr.Index -> {
                val target = evalExpr(expr.target, scope)
                val idx = (evalExpr(expr.index, scope) as? Number)?.toInt() ?: 0
                when (target) {
                    is MutableList<*> -> (target as MutableList<Any?>).getOrNull(idx)
                    is List<*> -> target.getOrNull(idx)
                    is Array<*> -> target.getOrNull(idx)
                    is String -> target.getOrNull(idx)?.toString()
                    else -> throw RuntimeException("Indexador inválido para ${target?.javaClass?.simpleName}")
                }
            }
            is Expr.New -> evalNew(expr, scope)
            is Expr.Call -> evalCall(expr, scope)
        }
    }

    private suspend fun evalBinary(expr: Expr.Binary, scope: Scope): Any? {
        val left = evalExpr(expr.left, scope)
        // Short-circuit logic for && and ||
        if (expr.op == "&&") {
            return if (!isTruthy(left)) false else isTruthy(evalExpr(expr.right, scope))
        }
        if (expr.op == "||") {
            return if (isTruthy(left)) true else isTruthy(evalExpr(expr.right, scope))
        }

        val right = evalExpr(expr.right, scope)

        return when (expr.op) {
            "+" -> {
                if (left is String || right is String) {
                    "${left ?: "null"}${right ?: "null"}"
                } else if (left is Double || right is Double || left is Float || right is Float) {
                    ((left as? Number)?.toDouble() ?: 0.0) + ((right as? Number)?.toDouble() ?: 0.0)
                } else {
                    ((left as? Number)?.toLong() ?: 0L) + ((right as? Number)?.toLong() ?: 0L)
                }
            }
            "-" -> {
                if (left is Double || right is Double) {
                    ((left as? Number)?.toDouble() ?: 0.0) - ((right as? Number)?.toDouble() ?: 0.0)
                } else {
                    ((left as? Number)?.toLong() ?: 0L) - ((right as? Number)?.toLong() ?: 0L)
                }
            }
            "*" -> {
                if (left is Double || right is Double) {
                    ((left as? Number)?.toDouble() ?: 0.0) * ((right as? Number)?.toDouble() ?: 0.0)
                } else {
                    ((left as? Number)?.toLong() ?: 0L) * ((right as? Number)?.toLong() ?: 0L)
                }
            }
            "/" -> {
                val rNum = (right as? Number)?.toDouble() ?: 1.0
                if (rNum == 0.0) throw ArithmeticException("Divisão por zero.")
                if (left is Double || right is Double) {
                    ((left as? Number)?.toDouble() ?: 0.0) / rNum
                } else {
                    ((left as? Number)?.toLong() ?: 0L) / ((right as? Number)?.toLong() ?: 1L)
                }
            }
            "%" -> {
                val r = (right as? Number)?.toLong() ?: 1L
                if (r == 0L) throw ArithmeticException("Divisão por zero.")
                ((left as? Number)?.toLong() ?: 0L) % r
            }
            "==" -> left == right
            "!=" -> left != right
            "<" -> ((left as? Number)?.toDouble() ?: 0.0) < ((right as? Number)?.toDouble() ?: 0.0)
            "<=" -> ((left as? Number)?.toDouble() ?: 0.0) <= ((right as? Number)?.toDouble() ?: 0.0)
            ">" -> ((left as? Number)?.toDouble() ?: 0.0) > ((right as? Number)?.toDouble() ?: 0.0)
            ">=" -> ((left as? Number)?.toDouble() ?: 0.0) >= ((right as? Number)?.toDouble() ?: 0.0)
            else -> throw RuntimeException("Operador desconhecido: ${expr.op}")
        }
    }

    private suspend fun evalUnary(expr: Expr.Unary, scope: Scope): Any? {
        val op = expr.op
        if (op == "!") {
            val v = evalExpr(expr.operand, scope)
            return !isTruthy(v)
        }
        if (op == "-") {
            val v = evalExpr(expr.operand, scope)
            return if (v is Double) -v else -((v as? Number)?.toLong() ?: 0L)
        }
        if (op == "+") {
            return evalExpr(expr.operand, scope)
        }
        if (op == "++" || op == "--") {
            if (expr.operand is Expr.Identifier) {
                val varName = expr.operand.name
                val oldVal = (scope.get(varName) as? Number)?.toLong() ?: 0L
                val newVal = if (op == "++") oldVal + 1 else oldVal - 1
                scope.assign(varName, newVal)
                return if (expr.isPrefix) newVal else oldVal
            }
        }
        return null
    }

    private suspend fun evalMemberAccess(expr: Expr.MemberAccess, scope: Scope): Any? {
        val target = evalExpr(expr.target, scope)
        val member = expr.member

        // Static classes / APIs
        if (target == "Math") {
            return when (member) {
                "PI" -> Math.PI
                "E" -> Math.E
                else -> "Math.$member"
            }
        }
        if (target == "DateTime") {
            return when (member) {
                "Now" -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                "Today" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                "UtcNow" -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.getDefault()).format(Date())
                else -> "DateTime.$member"
            }
        }
        if (target == "Console") {
            return "Console.$member"
        }
        if (target == "Convert") {
            return "Convert.$member"
        }

        // Instance properties / methods
        if (target is CSharpInstance) {
            return target.fields[member]
        }
        if (target is String) {
            return when (member) {
                "Length" -> target.length
                else -> target
            }
        }
        if (target is List<*>) {
            return when (member) {
                "Count", "Length" -> target.size
                else -> target
            }
        }
        if (target is Array<*>) {
            return when (member) {
                "Length" -> target.size
                else -> target
            }
        }

        return null
    }

    private suspend fun evalNew(expr: Expr.New, scope: Scope): Any? {
        val evaluatedArgs = expr.args.map { evalExpr(it, scope) }
        val evaluatedInits = expr.initializer?.map { evalExpr(it, scope) }

        if (expr.typeName.startsWith("List")) {
            val list = mutableListOf<Any?>()
            if (evaluatedInits != null) list.addAll(evaluatedInits)
            return list
        }

        if (expr.typeName == "Array" || expr.typeName.endsWith("[]")) {
            val size = if (evaluatedArgs.isNotEmpty()) (evaluatedArgs[0] as? Number)?.toInt() ?: 0 else (evaluatedInits?.size ?: 0)
            val arr = arrayOfNulls<Any?>(size)
            if (evaluatedInits != null) {
                for (i in 0 until min(size, evaluatedInits.size)) {
                    arr[i] = evaluatedInits[i]
                }
            }
            return arr
        }

        // Check if user defined class
        val classDecl = classes[expr.typeName]
        if (classDecl != null) {
            val instance = CSharpInstance(classDecl.name)
            for (f in classDecl.fields) {
                val initVal = f.init?.let { evalExpr(it, scope) } ?: defaultTypeValue(f.typeName)
                instance.fields[f.name] = initVal
            }
            return instance
        }

        return CSharpInstance(expr.typeName)
    }

    private suspend fun evalCall(expr: Expr.Call, scope: Scope): Any? {
        val evaluatedArgs = expr.args.map { evalExpr(it, scope) }

        // Method calls like Console.WriteLine, Math.Sqrt, etc.
        if (expr.callee is Expr.MemberAccess) {
            val obj = evalExpr(expr.callee.target, scope)
            val method = expr.callee.member

            if (obj == "Console" || (expr.callee.target is Expr.Identifier && (expr.callee.target as Expr.Identifier).name == "Console")) {
                when (method) {
                    "WriteLine" -> {
                        val text = evaluatedArgs.joinToString(" ") { it?.toString() ?: "null" }
                        onOutput(ConsoleEntry(type = ConsoleEntryType.STDOUT, text = text))
                        return null
                    }
                    "Write" -> {
                        val text = evaluatedArgs.joinToString(" ") { it?.toString() ?: "null" }
                        onOutput(ConsoleEntry(type = ConsoleEntryType.STDOUT, text = text))
                        return null
                    }
                    "ReadLine" -> {
                        onOutput(ConsoleEntry(type = ConsoleEntryType.DEBUG, text = "[Aguardando entrada do console...]"))
                        onStateChange(DebugState.WAITING_INPUT, currentLine, callStackFrames.toList())
                        val input = inputChannel.receive()
                        onOutput(ConsoleEntry(type = ConsoleEntryType.STDIN, text = "> $input"))
                        onStateChange(if (isDebug) DebugState.PAUSED else DebugState.RUNNING, currentLine, callStackFrames.toList())
                        return input
                    }
                    "Clear" -> {
                        onConsoleClear()
                        return null
                    }
                }
            }

            if (obj == "Math") {
                val d0 = (evaluatedArgs.getOrNull(0) as? Number)?.toDouble() ?: 0.0
                val d1 = (evaluatedArgs.getOrNull(1) as? Number)?.toDouble() ?: 0.0
                return when (method) {
                    "Sqrt" -> sqrt(d0)
                    "Pow" -> d0.pow(d1)
                    "Max" -> max(d0, d1)
                    "Min" -> min(d0, d1)
                    "Abs" -> abs(d0)
                    "Round" -> round(d0)
                    "Floor" -> floor(d0)
                    "Ceiling" -> ceil(d0)
                    else -> 0.0
                }
            }

            if (obj == "Convert") {
                val first = evaluatedArgs.getOrNull(0)
                return when (method) {
                    "ToInt32" -> (first as? Number)?.toLong() ?: first?.toString()?.toLongOrNull() ?: 0L
                    "ToDouble" -> (first as? Number)?.toDouble() ?: first?.toString()?.toDoubleOrNull() ?: 0.0
                    "ToString" -> first?.toString() ?: "null"
                    else -> first
                }
            }

            // String instance methods
            if (obj is String) {
                return when (method) {
                    "ToUpper" -> obj.uppercase(Locale.getDefault())
                    "ToLower" -> obj.lowercase(Locale.getDefault())
                    "Trim" -> obj.trim()
                    "Substring" -> {
                        val start = (evaluatedArgs.getOrNull(0) as? Number)?.toInt() ?: 0
                        val len = (evaluatedArgs.getOrNull(1) as? Number)?.toInt()
                        if (len != null) obj.substring(start, (start + len).coerceAtMost(obj.length))
                        else obj.substring(start)
                    }
                    "Contains" -> obj.contains(evaluatedArgs.getOrNull(0)?.toString() ?: "")
                    "IndexOf" -> obj.indexOf(evaluatedArgs.getOrNull(0)?.toString() ?: "")
                    "Replace" -> obj.replace(evaluatedArgs.getOrNull(0)?.toString() ?: "", evaluatedArgs.getOrNull(1)?.toString() ?: "")
                    "ToString" -> obj
                    else -> obj
                }
            }

            // List instance methods
            if (obj is MutableList<*>) {
                val list = obj as MutableList<Any?>
                return when (method) {
                    "Add" -> {
                        list.add(evaluatedArgs.getOrNull(0))
                        null
                    }
                    "Remove" -> {
                        list.remove(evaluatedArgs.getOrNull(0))
                        null
                    }
                    "Clear" -> {
                        list.clear()
                        null
                    }
                    "Contains" -> list.contains(evaluatedArgs.getOrNull(0))
                    "ToString" -> "[${list.joinToString(", ")}]"
                    else -> null
                }
            }

            // User object method call
            if (obj is CSharpInstance) {
                val classDecl = classes[obj.className]
                val methodDecl = classDecl?.methods?.find { it.name == method }
                if (methodDecl != null) {
                    return invokeFunction(methodDecl, evaluatedArgs, obj)
                }
            }
        }

        // Direct function call (e.g. Fib(n), Add(a, b))
        if (expr.callee is Expr.Identifier) {
            val name = expr.callee.name
            if (name == "int.Parse" || (name == "Parse")) {
                return evaluatedArgs.getOrNull(0)?.toString()?.toLongOrNull() ?: 0L
            }
            if (name == "double.Parse") {
                return evaluatedArgs.getOrNull(0)?.toString()?.toDoubleOrNull() ?: 0.0
            }

            val func = functions[name]
            if (func != null) {
                return invokeFunction(func, evaluatedArgs, null)
            }
        }

        throw RuntimeException("Função desconhecida: ${expr.callee}")
    }

    private suspend fun invokeFunction(func: Statement.FunctionDecl, args: List<Any?>, thisObj: CSharpInstance?): Any? {
        val funcScope = Scope()
        if (thisObj != null) {
            funcScope.define("this", thisObj.className, thisObj)
            thisObj.fields.forEach { (k, v) ->
                funcScope.define(k, "var", v)
            }
        }
        for (i in func.params.indices) {
            val param = func.params[i]
            val argVal = args.getOrNull(i) ?: defaultTypeValue(param.type)
            funcScope.define(param.name, param.type, argVal)
        }

        pushFrame(func.name, thisObj?.className ?: "Program", func.line, funcScope)

        var result: Any? = null
        try {
            for (stmt in func.body.statements) {
                executeStatement(stmt, funcScope)
            }
        } catch (e: ReturnException) {
            result = e.value
        } finally {
            // If thisObj fields were modified in method
            if (thisObj != null) {
                thisObj.fields.keys.forEach { k ->
                    thisObj.fields[k] = funcScope.get(k)
                }
            }
            popFrame()
        }
        return result
    }

    private fun applyAssignment(target: Expr, op: String, rightVal: Any?, scope: Scope) {
        when (target) {
            is Expr.Identifier -> {
                val oldVal = scope.get(target.name)
                val finalVal = when (op) {
                    "=" -> rightVal
                    "+=" -> if (oldVal is String || rightVal is String) "${oldVal ?: ""}${rightVal ?: ""}" else ((oldVal as? Number)?.toDouble() ?: 0.0) + ((rightVal as? Number)?.toDouble() ?: 0.0)
                    "-=" -> ((oldVal as? Number)?.toDouble() ?: 0.0) - ((rightVal as? Number)?.toDouble() ?: 0.0)
                    "*=" -> ((oldVal as? Number)?.toDouble() ?: 0.0) * ((rightVal as? Number)?.toDouble() ?: 0.0)
                    "/=" -> ((oldVal as? Number)?.toDouble() ?: 0.0) / ((rightVal as? Number)?.toDouble() ?: 1.0)
                    "%=" -> ((oldVal as? Number)?.toLong() ?: 0L) % ((rightVal as? Number)?.toLong() ?: 1L)
                    else -> rightVal
                }
                val castVal = if (oldVal is Long || oldVal is Int) (finalVal as? Number)?.toLong() else finalVal
                scope.assign(target.name, castVal)
            }
            is Expr.Index -> {
                val container = scope.get((target.target as? Expr.Identifier)?.name ?: "")
                val idx = (target.index as? Expr.Literal)?.value as? Number
                val indexInt = idx?.toInt() ?: 0
                if (container is MutableList<*>) {
                    (container as MutableList<Any?>)[indexInt] = rightVal
                } else if (container is Array<*>) {
                    (container as Array<Any?>)[indexInt] = rightVal
                }
            }
            is Expr.MemberAccess -> {
                val obj = scope.get((target.target as? Expr.Identifier)?.name ?: "")
                if (obj is CSharpInstance) {
                    obj.fields[target.member] = rightVal
                }
            }
            else -> {}
        }
    }

    private fun isTruthy(v: Any?): Boolean {
        return when (v) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0
            is String -> v.isNotEmpty()
            else -> true
        }
    }

    private fun defaultTypeValue(type: String): Any? {
        return when (type) {
            "int", "long", "short", "byte" -> 0L
            "double", "float", "decimal" -> 0.0
            "bool" -> false
            "string" -> ""
            "char" -> '\u0000'
            else -> null
        }
    }

    /**
     * Mutate variable value in the active debugger frame during pause.
     */
    fun setVariableValue(varName: String, newValueStr: String): Boolean {
        val top = callStackFrames.lastOrNull() ?: return false
        val curr = top.variables[varName] ?: return false
        val parsed: Any? = when (curr.type) {
            "int", "long" -> newValueStr.toLongOrNull() ?: return false
            "double", "float" -> newValueStr.toDoubleOrNull() ?: return false
            "bool" -> newValueStr.toBooleanStrictOrNull() ?: return false
            "string" -> newValueStr.removeSurrounding("\"")
            else -> newValueStr
        }
        // Update top frame
        val updatedVars = top.variables.toMutableMap()
        updatedVars[varName] = curr.copy(value = parsed.toString(), rawValue = parsed)
        callStackFrames[callStackFrames.lastIndex] = top.copy(variables = updatedVars)
        onStateChange(DebugState.PAUSED, currentLine, callStackFrames.toList())
        return true
    }
}
