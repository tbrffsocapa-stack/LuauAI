package com.luauai.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luauai.ui.theme.*
import kotlinx.coroutines.delay

// ── Cores de syntax highlighting ─────────────────────────────────────────────
object LuauColors {
    val keyword   = Color(0xFF569CD6)   // azul
    val string    = Color(0xFFCE9178)   // laranja
    val comment   = Color(0xFF6A9955)   // verde
    val number    = Color(0xFFB5CEA8)   // verde claro
    val function_ = Color(0xFFDCDCAA)   // amarelo
    val type      = Color(0xFF4EC9B0)   // ciano
    val operator  = Color(0xFFD4D4D4)   // cinza claro
    val normal    = Color(0xFFD4D4D4)
    val lineNum   = Color(0xFF858585)
    val bg        = Color(0xFF1E1E1E)
    val headerBg  = Color(0xFF2D2D2D)
}

// Palavras-chave Luau
private val KEYWORDS = setOf(
    "local", "function", "end", "if", "then", "else", "elseif",
    "for", "while", "do", "repeat", "until", "return", "break",
    "continue", "and", "or", "not", "true", "false", "nil",
    "in", "pairs", "ipairs", "next", "select",
    "type", "typeof", "require", "pcall", "xpcall", "error",
    "assert", "print", "warn", "task", "game", "workspace",
    "script", "math", "table", "string", "coroutine",
    "Instance", "Vector3", "CFrame", "Color3", "UDim2", "UDim",
    "Enum", "Ray", "Region3", "NumberSequence", "ColorSequence"
)

@Composable
fun LuauCodeBlock(
    code: String,
    language: String = "lua",
    showLineNumbers: Boolean = true
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LuauColors.bg)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LuauColors.headerBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text  = language.uppercase(),
                color = LuauPrimary,
                style = MaterialTheme.typography.labelSmall
            )
            IconButton(
                onClick  = {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (copied) Icons.Default.Done else Icons.Default.ContentCopy,
                    contentDescription = if (copied) "Copiado" else "Copiar",
                    tint    = if (copied) Color(0xFF6A9955) else LuauOnSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ── Código ───────────────────────────────────────────────────────────
        val scrollState = rememberScrollState()
        SelectionContainer {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(12.dp)
            ) {
                // Numeração de linhas
                if (showLineNumbers) {
                    val lines = code.lines()
                    Column(modifier = Modifier.padding(end = 16.dp)) {
                        lines.forEachIndexed { i, _ ->
                            Text(
                                text  = (i + 1).toString(),
                                color = LuauColors.lineNum,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMono,
                                    fontSize   = 12.sp,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }

                // Código com highlight
                Text(
                    text  = highlightLuau(code),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMono,
                        fontSize   = 12.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

/**
 * Syntax highlighting para Luau.
 * Implementação manual — não depende de libs externas.
 */
fun highlightLuau(code: String): AnnotatedString = buildAnnotatedString {
    val lines = code.lines()

    lines.forEachIndexed { lineIdx, line ->
        var i = 0
        while (i < line.length) {
            val remaining = line.substring(i)

            when {
                // Comentário de linha --
                remaining.startsWith("--") -> {
                    withStyle(SpanStyle(color = LuauColors.comment)) {
                        append(line.substring(i))
                    }
                    i = line.length
                }

                // String com aspas duplas
                remaining.startsWith("\"") -> {
                    val end = findStringEnd(line, i, '"')
                    withStyle(SpanStyle(color = LuauColors.string)) {
                        append(line.substring(i, end))
                    }
                    i = end
                }

                // String com aspas simples
                remaining.startsWith("'") -> {
                    val end = findStringEnd(line, i, '\'')
                    withStyle(SpanStyle(color = LuauColors.string)) {
                        append(line.substring(i, end))
                    }
                    i = end
                }

                // String [[ ]]
                remaining.startsWith("[[") -> {
                    val closeIdx = line.indexOf("]]", i + 2)
                    val end = if (closeIdx >= 0) closeIdx + 2 else line.length
                    withStyle(SpanStyle(color = LuauColors.string)) {
                        append(line.substring(i, end))
                    }
                    i = end
                }

                // Número
                line[i].isDigit() || (line[i] == '.' && i + 1 < line.length && line[i+1].isDigit()) -> {
                    var j = i
                    while (j < line.length && (line[j].isDigit() || line[j] == '.' || line[j] == 'e' || line[j] == 'x')) j++
                    withStyle(SpanStyle(color = LuauColors.number)) {
                        append(line.substring(i, j))
                    }
                    i = j
                }

                // Identificador ou palavra-chave
                line[i].isLetter() || line[i] == '_' -> {
                    var j = i
                    while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                    val word = line.substring(i, j)

                    // Detectar "function" seguido de nome
                    val isFunction = line.substring(j).trimStart().startsWith("(") ||
                            (word != "function" && i > 0 && line.substring(0, i).trimEnd().endsWith("function"))

                    val color = when {
                        word in KEYWORDS -> LuauColors.keyword
                        isFunction && word != "function" -> LuauColors.function_
                        word[0].isUpperCase() -> LuauColors.type
                        else -> LuauColors.normal
                    }
                    withStyle(SpanStyle(color = color)) { append(word) }
                    i = j
                }

                else -> {
                    withStyle(SpanStyle(color = LuauColors.operator)) {
                        append(line[i])
                    }
                    i++
                }
            }
        }
        if (lineIdx < lines.lastIndex) append("\n")
    }
}

private fun findStringEnd(line: String, start: Int, quote: Char): Int {
    var i = start + 1
    while (i < line.length) {
        if (line[i] == '\\') { i += 2; continue }
        if (line[i] == quote) return i + 1
        i++
    }
    return line.length
}
