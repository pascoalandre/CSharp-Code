package com.example.csharp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.csharp.model.DebugState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CSharpIdeScreen(
    viewModel: CSharpIdeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { viewModel.setShowProjectPicker(true) }
                                .padding(vertical = 4.dp)
                                .testTag("top_bar_project_title")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CSharpPurple
                            ) {
                                Text(
                                    text = "C#",
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = uiState.currentProject.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = IdeTextWhite,
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Trocar projeto",
                                        tint = IdeTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = uiState.statusMessage,
                                    fontSize = 11.sp,
                                    color = when (uiState.debugState) {
                                        DebugState.PAUSED -> IdeYellow
                                        DebugState.RUNNING -> IdeGreen
                                        DebugState.ERROR -> IdeRed
                                        else -> IdeTextMuted
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        // Run button (Executar sem depurar)
                        IconButton(
                            onClick = { viewModel.runProgram(debugMode = false) },
                            modifier = Modifier.testTag("btn_top_run")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Executar código no console",
                                tint = IdeGreen
                            )
                        }

                        // Debug button (Depurar com breakpoints)
                        IconButton(
                            onClick = { viewModel.runProgram(debugMode = true) },
                            modifier = Modifier.testTag("btn_top_debug")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Depurar passo a passo",
                                tint = IdeYellow
                            )
                        }

                        // Save button
                        IconButton(
                            onClick = { viewModel.saveCurrentProject() },
                            modifier = Modifier.testTag("btn_top_save")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Salvar arquivo",
                                tint = IdeTeal
                            )
                        }

                        // Projects list button
                        IconButton(
                            onClick = { viewModel.setShowProjectPicker(true) },
                            modifier = Modifier.testTag("btn_top_projects")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Projetos e Exemplos",
                                tint = IdeTextWhite
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = IdeSurface
                    )
                )

                // Navigation Tabs: Editor, Console, Depurador
                TabRow(
                    selectedTabIndex = uiState.activeTab.ordinal,
                    containerColor = IdeSurfaceVariant,
                    contentColor = IdeTextWhite,
                    divider = {}
                ) {
                    Tab(
                        selected = uiState.activeTab == IdeTab.EDITOR,
                        onClick = { viewModel.setActiveTab(IdeTab.EDITOR) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Editor", fontSize = 13.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_editor")
                    )

                    Tab(
                        selected = uiState.activeTab == IdeTab.CONSOLE,
                        onClick = { viewModel.setActiveTab(IdeTab.CONSOLE) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Console", fontSize = 13.sp)
                                if (uiState.consoleEntries.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = IdeSurfaceBorder
                                    ) {
                                        Text(
                                            text = uiState.consoleEntries.size.toString(),
                                            fontSize = 10.sp,
                                            color = IdeTextMuted,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_console")
                    )

                    Tab(
                        selected = uiState.activeTab == IdeTab.DEBUGGER,
                        onClick = { viewModel.setActiveTab(IdeTab.DEBUGGER) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Depurador", fontSize = 13.sp)
                                if (uiState.debugState == DebugState.PAUSED) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(IdeYellow, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("tab_debugger")
                    )
                }

                Divider(color = IdeSurfaceBorder)
            }
        },
        bottomBar = {
            // Status bar showing line, col, debug state
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = IdeSurface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (uiState.debugState) {
                                DebugState.RUNNING -> IdeGreen
                                DebugState.PAUSED -> IdeYellow
                                DebugState.WAITING_INPUT -> IdeBlue
                                DebugState.ERROR -> IdeRed
                                else -> IdeTextMuted
                            }
                        ) {
                            Text(
                                text = when (uiState.debugState) {
                                    DebugState.RUNNING -> "RUN"
                                    DebugState.PAUSED -> "PAUSED"
                                    DebugState.WAITING_INPUT -> "INPUT"
                                    DebugState.ERROR -> "ERROR"
                                    DebugState.FINISHED -> "DONE"
                                    DebugState.IDLE -> "READY"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (uiState.currentExecutionLine > 0) "Linha de depuração: ${uiState.currentExecutionLine}" else "C# 12 (.NET Core Runtime)",
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 11.sp,
                            color = IdeTextMuted
                        )
                    }

                    Text(
                        text = "${uiState.code.lines().size} linhas | UTF-8",
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 11.sp,
                        color = IdeTextMuted
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                IdeTab.EDITOR -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        EditorView(
                            code = uiState.code,
                            cursorPosition = uiState.cursorPosition,
                            breakpoints = uiState.breakpoints,
                            currentExecutionLine = uiState.currentExecutionLine,
                            onCodeChange = viewModel::onCodeChange,
                            onToggleBreakpoint = viewModel::toggleBreakpoint,
                            modifier = Modifier.weight(1f)
                        )

                        CodeToolbar(
                            suggestions = uiState.suggestions,
                            onSelectSuggestion = viewModel::insertSuggestion,
                            onInsertToken = viewModel::insertToken,
                            onFormatCode = viewModel::formatCode,
                            onUndo = viewModel::undo,
                            onRedo = viewModel::redo,
                            canUndo = uiState.canUndo,
                            canRedo = uiState.canRedo
                        )
                    }
                }

                IdeTab.CONSOLE -> {
                    ConsoleView(
                        entries = uiState.consoleEntries,
                        isWaitingInput = uiState.isConsoleWaitingInput,
                        inputText = uiState.consoleInputText,
                        debugState = uiState.debugState,
                        onInputTextChange = viewModel::updateConsoleInputText,
                        onSendInput = viewModel::sendConsoleInput,
                        onClearConsole = viewModel::clearConsole,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                IdeTab.DEBUGGER -> {
                    DebuggerView(
                        debugState = uiState.debugState,
                        currentLine = uiState.currentExecutionLine,
                        callStack = uiState.callStack,
                        variables = uiState.localVariables,
                        watchList = uiState.watchList,
                        breakpoints = uiState.breakpoints,
                        onStepOver = viewModel::stepOver,
                        onStepInto = viewModel::stepInto,
                        onStepOut = viewModel::stepOut,
                        onContinue = viewModel::continueExecution,
                        onPause = viewModel::pauseExecution,
                        onStop = viewModel::stopExecution,
                        onEditVariable = { name, currVal -> viewModel.setEditVariableDialog(name, currVal) },
                        onAddWatch = { viewModel.setShowAddWatchDialog(true) },
                        onRemoveWatch = viewModel::removeWatchItem,
                        onToggleBreakpoint = viewModel::toggleBreakpoint,
                        onClearBreakpoints = viewModel::clearAllBreakpoints,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Dialogs & Sheets
    if (uiState.showProjectPicker) {
        ProjectSelectorSheet(
            projects = uiState.allProjects,
            currentProject = uiState.currentProject,
            onSelectProject = viewModel::selectProject,
            onNewProject = {
                viewModel.setShowProjectPicker(false)
                viewModel.setShowNewProjectDialog(true)
            },
            onDeleteProject = viewModel::deleteProject,
            onDismiss = { viewModel.setShowProjectPicker(false) }
        )
    }

    if (uiState.showNewProjectDialog) {
        NewProjectDialog(
            onConfirm = viewModel::createNewProject,
            onDismiss = { viewModel.setShowNewProjectDialog(false) }
        )
    }

    if (uiState.showAddWatchDialog) {
        AddWatchDialog(
            onConfirm = viewModel::addWatchItem,
            onDismiss = { viewModel.setShowAddWatchDialog(false) }
        )
    }

    uiState.showEditVarDialog?.let { (varName, currVal) ->
        EditVariableDialog(
            varName = varName,
            currentValue = currVal,
            onConfirm = { newVal -> viewModel.updateVariableValue(varName, newVal) },
            onDismiss = { viewModel.setEditVariableDialog(null, null) }
        )
    }
}
