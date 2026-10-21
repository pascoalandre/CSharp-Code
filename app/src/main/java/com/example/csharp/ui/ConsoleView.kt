package com.example.csharp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csharp.model.ConsoleEntry
import com.example.csharp.model.ConsoleEntryType
import com.example.csharp.model.DebugState
import com.example.ui.theme.*

@Composable
fun ConsoleView(
    entries: List<ConsoleEntry>,
    isWaitingInput: Boolean,
    inputText: String,
    debugState: DebugState,
    onInputTextChange: (String) -> Unit,
    onSendInput: (String) -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Auto-scroll when new items arrive
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
    ) {
        // 1. Console Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IdeSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = IdeTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Terminal do Console",
                    style = MaterialTheme.typography.titleMedium,
                    color = IdeTextWhite
                )
                Spacer(Modifier.width(8.dp))
                // Status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (debugState) {
                        DebugState.RUNNING -> IdeGreen.copy(alpha = 0.2f)
                        DebugState.PAUSED -> IdeYellow.copy(alpha = 0.2f)
                        DebugState.WAITING_INPUT -> IdeBlue.copy(alpha = 0.2f)
                        DebugState.ERROR -> IdeRed.copy(alpha = 0.2f)
                        else -> IdeSurfaceVariant
                    }
                ) {
                    Text(
                        text = when (debugState) {
                            DebugState.RUNNING -> "Em execução"
                            DebugState.PAUSED -> "Pausado"
                            DebugState.WAITING_INPUT -> "Entrada aguardada"
                            DebugState.ERROR -> "Erro"
                            DebugState.FINISHED -> "Finalizado"
                            DebugState.IDLE -> "Ocioso"
                        },
                        fontSize = 11.sp,
                        color = when (debugState) {
                            DebugState.RUNNING -> IdeGreen
                            DebugState.PAUSED -> IdeYellow
                            DebugState.WAITING_INPUT -> IdeBlue
                            DebugState.ERROR -> IdeRed
                            else -> IdeTextMuted
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row {
                IconButton(
                    onClick = {
                        val fullLog = entries.joinToString("\n") { it.text }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Console C#", fullLog))
                        Toast.makeText(context, "Log copiado para a área de transferência", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_copy_console")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar saída",
                        tint = IdeTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClearConsole,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("btn_clear_console")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpar console",
                        tint = IdeTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Divider(color = IdeSurfaceBorder)

        // 2. Terminal Log List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = IdeSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Nenhuma saída no console ainda.",
                        color = IdeTextMuted,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Clique em Executar ou Depurar para rodar o código C#.",
                        color = IdeTextMuted.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("console_logs_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        Text(
                            text = entry.text,
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = when (entry.type) {
                                ConsoleEntryType.STDOUT -> IdeTextWhite
                                ConsoleEntryType.STDERR -> IdeRed
                                ConsoleEntryType.STDIN -> IdeTeal
                                ConsoleEntryType.SYSTEM -> IdeTextMuted
                                ConsoleEntryType.DEBUG -> IdeYellow
                            }
                        )
                    }
                }
            }
        }

        // 3. Interactive Input Bar for Console.ReadLine()
        AnimatedVisibility(visible = isWaitingInput || debugState == DebugState.RUNNING) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = IdeSurface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ">",
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 16.sp,
                        color = IdeTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                text = if (isWaitingInput) "Digite entrada para Console.ReadLine()..." else "Entrada do console...",
                                color = IdeTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 13.sp,
                            color = IdeTextWhite
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotEmpty() || isWaitingInput) {
                                onSendInput(inputText)
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CSharpPurple,
                            unfocusedBorderColor = IdeSurfaceBorder,
                            focusedContainerColor = IdeBackground,
                            unfocusedContainerColor = IdeBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("console_input_field")
                    )

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            onSendInput(inputText)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = CSharpPurple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_send_console_input")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar entrada",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
