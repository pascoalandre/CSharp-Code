package com.example.csharp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.csharp.model.*
import com.example.ui.theme.*

enum class DebuggerSubTab {
    LOCALS,
    WATCH,
    CALL_STACK,
    BREAKPOINTS
}

@Composable
fun DebuggerView(
    debugState: DebugState,
    currentLine: Int,
    callStack: List<StackFrame>,
    variables: Map<String, CSharpVariable>,
    watchList: List<WatchItem>,
    breakpoints: Set<Int>,
    onStepOver: () -> Unit,
    onStepInto: () -> Unit,
    onStepOut: () -> Unit,
    onContinue: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onEditVariable: (String, String) -> Unit,
    onAddWatch: () -> Unit,
    onRemoveWatch: (String) -> Unit,
    onToggleBreakpoint: (Int) -> Unit,
    onClearBreakpoints: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(DebuggerSubTab.LOCALS) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
    ) {
        // 1. Debugger Toolbar Actions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = IdeSurface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (debugState) {
                                    DebugState.RUNNING -> IdeGreen
                                    DebugState.PAUSED -> IdeYellow
                                    DebugState.WAITING_INPUT -> IdeBlue
                                    DebugState.ERROR -> IdeRed
                                    else -> IdeTextMuted
                                },
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (debugState == DebugState.PAUSED && currentLine > 0) "Linha $currentLine" else debugState.name,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 12.sp,
                        color = IdeTextWhite,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Controls: Continue, Step Over, Step Into, Step Out, Pause, Stop
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPaused = (debugState == DebugState.PAUSED)
                    val isRunning = (debugState == DebugState.RUNNING)
                    val isActive = isPaused || isRunning

                    // Continue / Run
                    IconButton(
                        onClick = onContinue,
                        enabled = isPaused,
                        modifier = Modifier.size(36.dp).testTag("btn_debug_continue")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Continuar (F5)", tint = if (isPaused) IdeGreen else IdeTextMuted)
                    }

                    // Step Over
                    IconButton(
                        onClick = onStepOver,
                        enabled = isPaused,
                        modifier = Modifier.size(36.dp).testTag("btn_debug_step_over")
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Passo Sobre (F10)", tint = if (isPaused) IdeBlue else IdeTextMuted)
                    }

                    // Step Into
                    IconButton(
                        onClick = onStepInto,
                        enabled = isPaused,
                        modifier = Modifier.size(36.dp).testTag("btn_debug_step_into")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Passo Dentro (F11)", tint = if (isPaused) IdeTeal else IdeTextMuted)
                    }

                    // Step Out
                    IconButton(
                        onClick = onStepOut,
                        enabled = isPaused,
                        modifier = Modifier.size(36.dp).testTag("btn_debug_step_out")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Passo Fora (Shift+F11)", tint = if (isPaused) IdeOrange else IdeTextMuted)
                    }

                    // Pause
                    IconButton(
                        onClick = onPause,
                        enabled = isRunning,
                        modifier = Modifier.size(36.dp).testTag("btn_debug_pause")
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = if (isRunning) IdeYellow else IdeTextMuted)
                    }

                    // Stop
                    IconButton(
                        onClick = onStop,
                        enabled = isActive,
                        modifier = Modifier.size(36.dp).testTag("btn_debug_stop")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Parar (Shift+F5)", tint = if (isActive) IdeRed else IdeTextMuted)
                    }
                }
            }
        }

        Divider(color = IdeSurfaceBorder)

        // 2. Sub-Tabs: Locals, Watch, Call Stack, Breakpoints
        TabRow(
            selectedTabIndex = selectedSubTab.ordinal,
            containerColor = IdeSurfaceVariant,
            contentColor = IdeTextWhite,
            divider = {}
        ) {
            Tab(
                selected = selectedSubTab == DebuggerSubTab.LOCALS,
                onClick = { selectedSubTab = DebuggerSubTab.LOCALS },
                text = { Text("Locais (${variables.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == DebuggerSubTab.WATCH,
                onClick = { selectedSubTab = DebuggerSubTab.WATCH },
                text = { Text("Watch (${watchList.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == DebuggerSubTab.CALL_STACK,
                onClick = { selectedSubTab = DebuggerSubTab.CALL_STACK },
                text = { Text("Pilha (${callStack.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSubTab == DebuggerSubTab.BREAKPOINTS,
                onClick = { selectedSubTab = DebuggerSubTab.BREAKPOINTS },
                text = { Text("Pontos (${breakpoints.size})", fontSize = 12.sp) }
            )
        }

        // 3. Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (selectedSubTab) {
                DebuggerSubTab.LOCALS -> LocalsPanel(
                    variables = variables,
                    onEditVariable = onEditVariable
                )
                DebuggerSubTab.WATCH -> WatchPanel(
                    watchList = watchList,
                    onAddWatch = onAddWatch,
                    onRemoveWatch = onRemoveWatch
                )
                DebuggerSubTab.CALL_STACK -> CallStackPanel(
                    callStack = callStack
                )
                DebuggerSubTab.BREAKPOINTS -> BreakpointsPanel(
                    breakpoints = breakpoints,
                    onToggleBreakpoint = onToggleBreakpoint,
                    onClearBreakpoints = onClearBreakpoints
                )
            }
        }
    }
}

@Composable
private fun LocalsPanel(
    variables: Map<String, CSharpVariable>,
    onEditVariable: (String, String) -> Unit
) {
    if (variables.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Nenhuma variável local no escopo atual.\nPause em um breakpoint para inspecionar.",
                color = IdeTextMuted,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("locals_variables_list"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(variables.values.toList(), key = { it.name }) { v ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditVariable(v.name, v.value) }
                        .testTag("var_item_${v.name}"),
                    colors = CardDefaults.cardColors(containerColor = IdeSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = v.name,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = IdeBlue
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = v.type,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = IdeTeal
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = v.value,
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 13.sp,
                                color = IdeTextWhite
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar valor",
                            tint = IdeTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchPanel(
    watchList: List<WatchItem>,
    onAddWatch: () -> Unit,
    onRemoveWatch: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Expressões Monitoradas",
                style = MaterialTheme.typography.titleMedium,
                color = IdeTextWhite
            )
            FilledTonalButton(
                onClick = onAddWatch,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CSharpPurple),
                modifier = Modifier.testTag("btn_add_watch")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Adicionar Watch", fontSize = 12.sp, color = Color.White)
            }
        }

        if (watchList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nenhuma expressão monitorada.\nToque em 'Adicionar Watch' para monitorar.",
                    color = IdeTextMuted,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("watch_items_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(watchList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.expression,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = IdeYellow
                                )
                                Text(
                                    text = if (item.value.isEmpty()) "<fora do escopo>" else item.value,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 13.sp,
                                    color = if (item.value.isEmpty()) IdeTextMuted else IdeTextWhite
                                )
                            }
                            IconButton(
                                onClick = { onRemoveWatch(item.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover", tint = IdeRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallStackPanel(callStack: List<StackFrame>) {
    if (callStack.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pilha de chamadas vazia.", color = IdeTextMuted, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("call_stack_list"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(callStack.reversed()) { frame ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = IdeSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = CSharpPurpleLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${frame.className}.${frame.functionName}()",
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = IdeTextWhite
                            )
                            Text(
                                text = "Linha ${frame.lineNumber}",
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 11.sp,
                                color = IdeTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakpointsPanel(
    breakpoints: Set<Int>,
    onToggleBreakpoint: (Int) -> Unit,
    onClearBreakpoints: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pontos de Interrupção Ativos",
                style = MaterialTheme.typography.titleMedium,
                color = IdeTextWhite
            )
            if (breakpoints.isNotEmpty()) {
                TextButton(onClick = onClearBreakpoints) {
                    Text("Remover Todos", color = IdeRed, fontSize = 12.sp)
                }
            }
        }

        if (breakpoints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nenhum ponto de interrupção definido.\nToque na barra lateral do editor para adicionar.",
                    color = IdeTextMuted,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(breakpoints.sorted()) { line ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = IdeSurface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(IdeRed, RoundedCornerShape(5.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Linha $line",
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 13.sp,
                                    color = IdeTextWhite
                                )
                            }
                            IconButton(
                                onClick = { onToggleBreakpoint(line) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = IdeTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
