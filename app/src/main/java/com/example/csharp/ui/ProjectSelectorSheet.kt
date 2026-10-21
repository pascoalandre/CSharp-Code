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
import com.example.csharp.model.CSharpProject
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectorSheet(
    projects: List<CSharpProject>,
    currentProject: CSharpProject,
    onSelectProject: (CSharpProject) -> Unit,
    onNewProject: () -> Unit,
    onDeleteProject: (CSharpProject) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = IdeSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = IdeSurfaceBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projetos & Exemplos C#",
                    style = MaterialTheme.typography.titleLarge,
                    color = IdeTextWhite
                )

                Button(
                    onClick = onNewProject,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CSharpPurple),
                    modifier = Modifier.testTag("btn_new_project_sheet")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Novo", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects, key = { "${it.id}_${it.title}" }) { proj ->
                    val isSelected = (proj.id == currentProject.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProject(proj) }
                            .testTag("project_item_${proj.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) IdeSurfaceVariant else IdeBackground
                        ),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CSharpPurple)) else null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = proj.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = IdeTextWhite
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    if (proj.isSample) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = IdeTeal.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Exemplo",
                                                color = IdeTeal,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (proj.description.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = proj.description,
                                        color = IdeTextMuted,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            if (!proj.isSample) {
                                IconButton(
                                    onClick = { onDeleteProject(proj) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir projeto",
                                        tint = IdeRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditVariableDialog(
    varName: String,
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var valueText by remember { mutableStateOf(currentValue.removeSurrounding("\"")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar valor de '$varName'", color = IdeTextWhite) },
        text = {
            Column {
                Text(
                    "Insira o novo valor para a variável durante a pausa da depuração:",
                    color = IdeTextMuted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = RobotoMonoFontFamily, color = IdeTextWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CSharpPurple,
                        unfocusedBorderColor = IdeSurfaceBorder,
                        focusedContainerColor = IdeSurface,
                        unfocusedContainerColor = IdeSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_var_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(valueText) },
                colors = ButtonDefaults.buttonColors(containerColor = CSharpPurple),
                modifier = Modifier.testTag("btn_confirm_edit_var")
            ) {
                Text("Atualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = IdeTextMuted)
            }
        },
        containerColor = IdeSurfaceVariant
    )
}

@Composable
fun AddWatchDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var exprText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Expressão Watch", color = IdeTextWhite) },
        text = {
            Column {
                Text(
                    "Digite uma variável ou expressão para inspecionar continuamente:",
                    color = IdeTextMuted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = exprText,
                    onValueChange = { exprText = it },
                    placeholder = { Text("ex: i, n, nome, saldo", color = IdeTextMuted) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = RobotoMonoFontFamily, color = IdeTextWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CSharpPurple,
                        unfocusedBorderColor = IdeSurfaceBorder,
                        focusedContainerColor = IdeSurface,
                        unfocusedContainerColor = IdeSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_watch_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(exprText) },
                colors = ButtonDefaults.buttonColors(containerColor = CSharpPurple),
                modifier = Modifier.testTag("btn_confirm_add_watch")
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = IdeTextMuted)
            }
        },
        containerColor = IdeSurfaceVariant
    )
}

@Composable
fun NewProjectDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var titleText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Arquivo C#", color = IdeTextWhite) },
        text = {
            Column {
                Text(
                    "Nome do programa ou arquivo:",
                    color = IdeTextMuted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = { Text("ex: MeuAlgoritmo.cs", color = IdeTextMuted) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = IdeTextWhite),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CSharpPurple,
                        unfocusedBorderColor = IdeSurfaceBorder,
                        focusedContainerColor = IdeSurface,
                        unfocusedContainerColor = IdeSurface
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_project_title_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(titleText) },
                colors = ButtonDefaults.buttonColors(containerColor = CSharpPurple),
                modifier = Modifier.testTag("btn_confirm_new_project")
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = IdeTextMuted)
            }
        },
        containerColor = IdeSurfaceVariant
    )
}
