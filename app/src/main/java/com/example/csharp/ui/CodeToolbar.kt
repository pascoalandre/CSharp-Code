package com.example.csharp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csharp.model.CodeSuggestion
import com.example.csharp.model.SuggestionKind
import com.example.ui.theme.*

@Composable
fun CodeToolbar(
    suggestions: List<CodeSuggestion>,
    onSelectSuggestion: (CodeSuggestion) -> Unit,
    onInsertToken: (String) -> Unit,
    onFormatCode: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(IdeSurface)
    ) {
        // 1. Autocomplete Suggestions Chips Row
        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IdeSurfaceVariant)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (suggestion in suggestions) {
                    SuggestionChip(
                        onClick = { onSelectSuggestion(suggestion) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (suggestion.kind) {
                                        SuggestionKind.SNIPPET -> "⚡ "
                                        SuggestionKind.METHOD -> "ƒ "
                                        SuggestionKind.CLASS -> "◇ "
                                        SuggestionKind.KEYWORD -> "k "
                                        SuggestionKind.VARIABLE -> "x "
                                        SuggestionKind.PROPERTY -> "p "
                                    },
                                    fontSize = 11.sp,
                                    color = when (suggestion.kind) {
                                        SuggestionKind.SNIPPET -> IdeYellow
                                        SuggestionKind.METHOD -> IdeYellow
                                        SuggestionKind.CLASS -> IdeTeal
                                        SuggestionKind.KEYWORD -> CSharpPurpleLight
                                        SuggestionKind.VARIABLE -> IdeBlue
                                        SuggestionKind.PROPERTY -> IdeOrange
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = suggestion.label,
                                    fontFamily = RobotoMonoFontFamily,
                                    fontSize = 13.sp,
                                    color = IdeTextWhite
                                )
                            }
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = IdeBackground
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = IdeSurfaceBorder
                        ),
                        modifier = Modifier.testTag("suggestion_${suggestion.label}")
                    )
                }
            }
        }

        // 2. Quick Symbol & Action Bar
        val symbols = listOf(
            ";", "{", "}", "(", ")", "[", "]", "\"", "'", "$",
            "=", "==", "+", "-", "*", "/", "!", ".",
            "cw", "cr", "for", "var", "    "
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo button
            FilledTonalButton(
                onClick = onUndo,
                enabled = canUndo,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IdeSurfaceVariant,
                    disabledContainerColor = IdeSurface
                ),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_undo")
            ) {
                Text("↶", fontSize = 14.sp)
            }

            // Redo button
            FilledTonalButton(
                onClick = onRedo,
                enabled = canRedo,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = IdeSurfaceVariant,
                    disabledContainerColor = IdeSurface
                ),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_redo")
            ) {
                Text("↷", fontSize = 14.sp)
            }

            // Format button
            FilledTonalButton(
                onClick = onFormatCode,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = IdeSurfaceVariant),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_format_code")
            ) {
                Text("{}", fontSize = 12.sp, color = IdeTeal, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text("Formatar", fontSize = 11.sp, color = IdeTextWhite)
            }

            Divider(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp),
                color = IdeSurfaceBorder
            )

            // Symbols
            for (sym in symbols) {
                FilledTonalButton(
                    onClick = {
                        when (sym) {
                            "cw" -> onInsertToken("Console.WriteLine();")
                            "cr" -> onInsertToken("Console.ReadLine();")
                            "for" -> onInsertToken("for (int i = 0; i < 10; i++) {\n    \n}")
                            "var" -> onInsertToken("var ")
                            else -> onInsertToken(sym)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = IdeSurfaceVariant),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_symbol_$sym")
                ) {
                    Text(
                        text = if (sym == "    ") "TAB" else sym,
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = if (sym.length > 2) 11.sp else 13.sp,
                        color = when (sym) {
                            "cw", "cr" -> IdeYellow
                            "for", "var" -> CSharpPurpleLight
                            else -> IdeTextWhite
                        },
                        fontWeight = if (sym.length > 2) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
