package com.example.csharp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.csharp.data.AppDatabase
import com.example.csharp.data.DefaultSamples
import com.example.csharp.data.ProjectEntity
import com.example.csharp.engine.CSharpAutocomplete
import com.example.csharp.engine.CSharpInterpreter
import com.example.csharp.engine.CSharpParser
import com.example.csharp.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class IdeTab {
    EDITOR,
    CONSOLE,
    DEBUGGER
}

data class IdeUiState(
    val activeTab: IdeTab = IdeTab.EDITOR,
    val currentProject: CSharpProject = DefaultSamples.SAMPLES.first(),
    val code: String = DefaultSamples.SAMPLES.first().code,
    val cursorPosition: Int = 0,
    val suggestions: List<CodeSuggestion> = emptyList(),
    val breakpoints: Set<Int> = setOf(14), // Default sample breakpoint on line 14
    val debugState: DebugState = DebugState.IDLE,
    val currentExecutionLine: Int = 0,
    val callStack: List<StackFrame> = emptyList(),
    val localVariables: Map<String, CSharpVariable> = emptyMap(),
    val watchList: List<WatchItem> = listOf(
        WatchItem(UUID.randomUUID().toString(), "nome"),
        WatchItem(UUID.randomUUID().toString(), "idade")
    ),
    val consoleEntries: List<ConsoleEntry> = emptyList(),
    val isConsoleWaitingInput: Boolean = false,
    val consoleInputText: String = "",
    val allProjects: List<CSharpProject> = emptyList(),
    val showProjectPicker: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val showEditVarDialog: Pair<String, String>? = null, // varName to currValue
    val showAddWatchDialog: Boolean = false,
    val statusMessage: String = "Pronto",
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class CSharpIdeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.projectDao()

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null
    private var interpreter: CSharpInterpreter? = null

    // Undo / Redo stacks
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = dao.count()
            if (count == 0) {
                DefaultSamples.SAMPLES.forEach {
                    dao.insertProject(ProjectEntity.fromDomain(it))
                }
            }

            dao.getAllProjects().collect { entities ->
                val projects = entities.map { it.toDomain() }
                _uiState.update { it.copy(allProjects = projects) }
            }
        }
    }

    fun setActiveTab(tab: IdeTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onCodeChange(newCode: String, newCursor: Int) {
        val oldCode = _uiState.value.code
        if (oldCode != newCode) {
            undoStack.add(oldCode)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }

        val suggestions = CSharpAutocomplete.getSuggestions(newCode, newCursor)

        _uiState.update {
            it.copy(
                code = newCode,
                cursorPosition = newCursor,
                suggestions = suggestions,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_uiState.value.code)
            _uiState.update {
                it.copy(
                    code = prev,
                    cursorPosition = prev.length.coerceAtMost(it.cursorPosition),
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = true
                )
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_uiState.value.code)
            _uiState.update {
                it.copy(
                    code = next,
                    cursorPosition = next.length.coerceAtMost(it.cursorPosition),
                    canUndo = true,
                    canRedo = redoStack.isNotEmpty()
                )
            }
        }
    }

    fun insertSuggestion(suggestion: CodeSuggestion) {
        val currCode = _uiState.value.code
        val cursor = _uiState.value.cursorPosition.coerceIn(0, currCode.length)

        // Find prefix to replace
        var start = cursor
        while (start > 0) {
            val c = currCode[start - 1]
            if (c.isLetterOrDigit() || c == '_' || c == '.') {
                start--
            } else {
                break
            }
        }

        val before = currCode.substring(0, start)
        val after = currCode.substring(cursor)
        val newCode = before + suggestion.insertText + after
        val newCursor = start + suggestion.cursorOffset

        onCodeChange(newCode, newCursor)
        _uiState.update { it.copy(suggestions = emptyList()) }
    }

    fun insertToken(token: String) {
        val currCode = _uiState.value.code
        val cursor = _uiState.value.cursorPosition.coerceIn(0, currCode.length)
        val before = currCode.substring(0, cursor)
        val after = currCode.substring(cursor)
        val newCode = before + token + after
        val newCursor = cursor + token.length
        onCodeChange(newCode, newCursor)
    }

    fun formatCode() {
        val raw = _uiState.value.code
        val lines = raw.lines()
        val formatted = StringBuilder()
        var indentLevel = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                formatted.append("\n")
                continue
            }
            if (trimmed.startsWith("}") || trimmed.startsWith(");")) {
                indentLevel = (indentLevel - 1).coerceAtLeast(0)
            }

            val indent = "    ".repeat(indentLevel)
            formatted.append(indent).append(trimmed).append("\n")

            if (trimmed.endsWith("{")) {
                indentLevel++
            }
        }

        val finalResult = formatted.toString().trimEnd() + "\n"
        onCodeChange(finalResult, _uiState.value.cursorPosition.coerceAtMost(finalResult.length))
        _uiState.update { it.copy(statusMessage = "Código formatado") }
    }

    fun toggleBreakpoint(line: Int) {
        _uiState.update { state ->
            val set = state.breakpoints.toMutableSet()
            if (set.contains(line)) {
                set.remove(line)
            } else {
                set.add(line)
            }
            state.copy(breakpoints = set)
        }
    }

    fun clearAllBreakpoints() {
        _uiState.update { it.copy(breakpoints = emptySet()) }
    }

    fun runProgram(debugMode: Boolean) {
        stopExecution()

        val sourceCode = _uiState.value.code
        _uiState.update {
            it.copy(
                debugState = DebugState.RUNNING,
                currentExecutionLine = 0,
                activeTab = if (debugMode) IdeTab.EDITOR else IdeTab.CONSOLE,
                statusMessage = if (debugMode) "Depurando C#..." else "Executando C#..."
            )
        }

        executionJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val parser = CSharpParser(sourceCode)
                val program = parser.parseProgram()

                val newInterpreter = CSharpInterpreter(
                    breakpointsProvider = { _uiState.value.breakpoints },
                    onOutput = { entry ->
                        _uiState.update { state ->
                            state.copy(consoleEntries = state.consoleEntries + entry)
                        }
                    },
                    onStateChange = { state, line, stack ->
                        _uiState.update { ui ->
                            val currentVars = stack.lastOrNull()?.variables ?: emptyMap()
                            val updatedWatches = ui.watchList.map { w ->
                                val v = currentVars[w.expression]
                                if (v != null) w.copy(value = v.value, hasError = false)
                                else w
                            }
                            ui.copy(
                                debugState = state,
                                currentExecutionLine = line,
                                callStack = stack,
                                localVariables = currentVars,
                                watchList = updatedWatches,
                                isConsoleWaitingInput = (state == DebugState.WAITING_INPUT),
                                statusMessage = when (state) {
                                    DebugState.PAUSED -> "Pausado na linha $line"
                                    DebugState.RUNNING -> "Executando..."
                                    DebugState.WAITING_INPUT -> "Aguardando entrada do console"
                                    DebugState.FINISHED -> "Execução concluída"
                                    DebugState.ERROR -> "Erro na execução"
                                    DebugState.IDLE -> "Pronto"
                                }
                            )
                        }
                    },
                    onConsoleClear = {
                        _uiState.update { it.copy(consoleEntries = emptyList()) }
                    }
                )

                interpreter = newInterpreter
                newInterpreter.execute(program, debugMode)

            } catch (e: CSharpParser.ParseException) {
                _uiState.update {
                    val entry = ConsoleEntry(type = ConsoleEntryType.STDERR, text = "Erro de Sintaxe: ${e.message}")
                    it.copy(
                        debugState = DebugState.ERROR,
                        consoleEntries = it.consoleEntries + entry,
                        statusMessage = "Erro de sintaxe",
                        activeTab = IdeTab.CONSOLE
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    val entry = ConsoleEntry(type = ConsoleEntryType.STDERR, text = "Erro: ${e.message}")
                    it.copy(
                        debugState = DebugState.ERROR,
                        consoleEntries = it.consoleEntries + entry,
                        statusMessage = "Erro ao iniciar execução",
                        activeTab = IdeTab.CONSOLE
                    )
                }
            }
        }
    }

    fun stepOver() {
        interpreter?.stepOver()
    }

    fun stepInto() {
        interpreter?.stepInto()
    }

    fun stepOut() {
        interpreter?.stepOut()
    }

    fun continueExecution() {
        interpreter?.continueExecution()
    }

    fun pauseExecution() {
        interpreter?.pauseExecution()
    }

    fun stopExecution() {
        interpreter?.stop()
        executionJob?.cancel()
        executionJob = null
        interpreter = null
        _uiState.update {
            it.copy(
                debugState = DebugState.IDLE,
                currentExecutionLine = 0,
                isConsoleWaitingInput = false,
                statusMessage = "Parado"
            )
        }
    }

    fun sendConsoleInput(text: String) {
        val input = text.trim()
        interpreter?.sendInput(input)
        _uiState.update { it.copy(consoleInputText = "", isConsoleWaitingInput = false) }
    }

    fun updateConsoleInputText(text: String) {
        _uiState.update { it.copy(consoleInputText = text) }
    }

    fun clearConsole() {
        _uiState.update { it.copy(consoleEntries = emptyList()) }
    }

    fun addWatchItem(expression: String) {
        val expr = expression.trim()
        if (expr.isEmpty()) return
        val currentVars = _uiState.value.localVariables
        val initialVal = currentVars[expr]?.value ?: ""
        val item = WatchItem(UUID.randomUUID().toString(), expr, initialVal)
        _uiState.update { it.copy(watchList = it.watchList + item, showAddWatchDialog = false) }
    }

    fun removeWatchItem(id: String) {
        _uiState.update { it.copy(watchList = it.watchList.filter { w -> w.id != id }) }
    }

    fun setEditVariableDialog(name: String?, value: String?) {
        _uiState.update {
            it.copy(showEditVarDialog = if (name != null && value != null) Pair(name, value) else null)
        }
    }

    fun updateVariableValue(name: String, newValue: String) {
        interpreter?.setVariableValue(name, newValue)
        _uiState.update { it.copy(showEditVarDialog = null) }
    }

    fun setShowProjectPicker(show: Boolean) {
        _uiState.update { it.copy(showProjectPicker = show) }
    }

    fun setShowNewProjectDialog(show: Boolean) {
        _uiState.update { it.copy(showNewProjectDialog = show) }
    }

    fun setShowAddWatchDialog(show: Boolean) {
        _uiState.update { it.copy(showAddWatchDialog = show) }
    }

    fun selectProject(project: CSharpProject) {
        stopExecution()
        undoStack.clear()
        redoStack.clear()
        _uiState.update {
            it.copy(
                currentProject = project,
                code = project.code,
                cursorPosition = 0,
                showProjectPicker = false,
                consoleEntries = emptyList(),
                currentExecutionLine = 0,
                debugState = DebugState.IDLE,
                statusMessage = "Carregado: ${project.title}"
            )
        }
    }

    fun saveCurrentProject() {
        val curr = _uiState.value.currentProject
        val code = _uiState.value.code
        val updated = curr.copy(code = code, updatedAt = System.currentTimeMillis())

        viewModelScope.launch(Dispatchers.IO) {
            dao.updateProject(ProjectEntity.fromDomain(updated))
            _uiState.update {
                it.copy(currentProject = updated, statusMessage = "Projeto salvo com sucesso!")
            }
        }
    }

    fun createNewProject(title: String) {
        val cleanTitle = title.trim().ifEmpty { "Novo Programa C#" }
        val templateCode = """using System;

class Program
{
    static void Main(string[] args)
    {
        Console.WriteLine("Olá do $cleanTitle!");
    }
}
"""
        val newProj = CSharpProject(
            title = cleanTitle,
            description = "Criado pelo usuário",
            code = templateCode,
            isSample = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            val id = dao.insertProject(ProjectEntity.fromDomain(newProj))
            val saved = newProj.copy(id = id)
            selectProject(saved)
            _uiState.update { it.copy(showNewProjectDialog = false) }
        }
    }

    fun deleteProject(project: CSharpProject) {
        if (project.isSample) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteProject(ProjectEntity.fromDomain(project))
            if (_uiState.value.currentProject.id == project.id) {
                val fallback = _uiState.value.allProjects.firstOrNull { it.id != project.id }
                    ?: DefaultSamples.SAMPLES.first()
                selectProject(fallback)
            }
        }
    }
}
