package com.example.csharp.model

/**
 * Representation of a C# project or source file.
 */
data class CSharpProject(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val code: String,
    val isSample: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Breakpoint on a specific source line.
 */
data class Breakpoint(
    val lineNumber: Int,
    val isEnabled: Boolean = true
)

/**
 * State of the C# execution and debugger.
 */
enum class DebugState {
    IDLE,
    RUNNING,
    PAUSED,
    WAITING_INPUT,
    FINISHED,
    ERROR
}

/**
 * Stepping mode for the debugger.
 */
enum class StepMode {
    NONE,
    STEP_OVER,
    STEP_INTO,
    STEP_OUT
}

/**
 * Stack frame representing a function invocation in C#.
 */
data class StackFrame(
    val functionName: String,
    val className: String = "Program",
    val lineNumber: Int,
    val variables: Map<String, CSharpVariable> = emptyMap()
)

/**
 * Variable inspected in debugger.
 */
data class CSharpVariable(
    val name: String,
    val type: String,
    val value: String,
    val rawValue: Any? = null
)

/**
 * Watch expression evaluated by the debugger.
 */
data class WatchItem(
    val id: String,
    val expression: String,
    val value: String = "",
    val hasError: Boolean = false
)

/**
 * Type of console output.
 */
enum class ConsoleEntryType {
    STDOUT,
    STDERR,
    STDIN,
    SYSTEM,
    DEBUG
}

/**
 * Log entry in the integrated console.
 */
data class ConsoleEntry(
    val id: Long = System.nanoTime(),
    val type: ConsoleEntryType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Autocomplete / Code suggestion item.
 */
data class CodeSuggestion(
    val label: String,
    val insertText: String,
    val detail: String,
    val kind: SuggestionKind,
    val cursorOffset: Int = insertText.length
)

enum class SuggestionKind {
    KEYWORD,
    SNIPPET,
    METHOD,
    CLASS,
    VARIABLE,
    PROPERTY
}
