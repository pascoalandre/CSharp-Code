package com.example.csharp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csharp.engine.CSharpSyntaxColors
import com.example.csharp.engine.CSharpSyntaxHighlighter
import com.example.ui.theme.*

class CSharpVisualTransformation(
    private val highlighter: CSharpSyntaxHighlighter
) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val highlighted = highlighter.highlight(text.text)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
fun EditorView(
    code: String,
    cursorPosition: Int,
    breakpoints: Set<Int>,
    currentExecutionLine: Int,
    onCodeChange: (String, Int) -> Unit,
    onToggleBreakpoint: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val highlighter = remember { CSharpSyntaxHighlighter() }
    val visualTransformation = remember { CSharpVisualTransformation(highlighter) }

    var textFieldValue by remember(code) {
        mutableStateOf(
            TextFieldValue(
                text = code,
                selection = TextRange(cursorPosition.coerceIn(0, code.length))
            )
        )
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    val lines = remember(code) { code.lines() }
    val totalLines = lines.size.coerceAtLeast(1)

    // Scroll to current execution line if paused
    LaunchedEffect(currentExecutionLine) {
        if (currentExecutionLine > 0) {
            val approximateScroll = (currentExecutionLine - 4).coerceAtLeast(0) * 24
            verticalScrollState.animateScrollTo(approximateScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
        ) {
            // 1. Line Numbers & Breakpoints Gutter
            Column(
                modifier = Modifier
                    .width(46.dp)
                    .background(IdeSurface)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 1..totalLines) {
                    val isBreakpoint = breakpoints.contains(i)
                    val isCurrentExecLine = (currentExecutionLine == i)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clickable { onToggleBreakpoint(i) }
                            .testTag("gutter_line_$i"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Current execution arrow
                        if (isCurrentExecLine) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Ponto de execução atual",
                                tint = IdeYellow,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isBreakpoint) {
                            // Red breakpoint dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(IdeRed, CircleShape)
                            )
                        } else {
                            // Regular Line number
                            Text(
                                text = i.toString(),
                                fontFamily = RobotoMonoFontFamily,
                                fontSize = 12.sp,
                                color = IdeTextMuted,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Gutter Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(IdeSurfaceBorder)
            )

            // 2. Code Area with Active Line Highlight & Horizontal Scroll
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(vertical = 12.dp, horizontal = 12.dp)
            ) {
                // Background highlight for paused execution line
                if (currentExecutionLine > 0) {
                    val topOffset = ((currentExecutionLine - 1) * 24).dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = topOffset)
                            .height(24.dp)
                            .background(IdeYellow.copy(alpha = 0.20f))
                    )
                }

                // Text Field with syntax transformation
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newVal ->
                        textFieldValue = newVal
                        onCodeChange(newVal.text, newVal.selection.start)
                    },
                    textStyle = TextStyle(
                        fontFamily = RobotoMonoFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 24.sp,
                        color = CSharpSyntaxColors.Default
                    ),
                    cursorBrush = SolidColor(CSharpPurple),
                    visualTransformation = visualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_editor_field")
                )
            }
        }
    }
}
