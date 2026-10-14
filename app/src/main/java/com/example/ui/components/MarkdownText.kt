package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyan

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val clipboardManager = LocalClipboardManager.current
    val segments = parseMarkdownSegments(markdown)

    Column(modifier = modifier) {
        segments.forEach { segment ->
            when (segment) {
                is MarkdownSegment.CodeBlock -> {
                    CodeBlockCard(
                        code = segment.code,
                        language = segment.language,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(segment.code))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is MarkdownSegment.Header -> {
                    Text(
                        text = segment.text,
                        color = JarvisCyan,
                        fontSize = if (segment.level == 1) 19.sp else if (segment.level == 2) 17.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownSegment.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = JarvisCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(segment.text, textColor),
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                is MarkdownSegment.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(segment.text, textColor),
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    code: String,
    language: String,
    onCopy: () -> Unit
) {
    val displayLang = if (language.isBlank()) "CODE" else language.uppercase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0F1D))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
    ) {
        // Header bar with language label and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111827))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayLang,
                color = JarvisCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                color = Color(0xFFE2E8F0),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

sealed class MarkdownSegment {
    data class Header(val text: String, val level: Int) : MarkdownSegment()
    data class CodeBlock(val code: String, val language: String) : MarkdownSegment()
    data class BulletItem(val text: String) : MarkdownSegment()
    data class Paragraph(val text: String) : MarkdownSegment()
}

fun parseMarkdownSegments(text: String): List<MarkdownSegment> {
    val segments = mutableListOf<MarkdownSegment>()
    val lines = text.lines()

    var inCodeBlock = false
    val codeBuilder = StringBuilder()
    var currentLanguage = ""

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // End of code block
                segments.add(
                    MarkdownSegment.CodeBlock(
                        code = codeBuilder.toString().trimEnd(),
                        language = currentLanguage
                    )
                )
                codeBuilder.clear()
                currentLanguage = ""
                inCodeBlock = false
            } else {
                // Start of code block
                inCodeBlock = true
                currentLanguage = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBuilder.append(line).append("\n")
            continue
        }

        if (trimmed.isEmpty()) continue

        when {
            trimmed.startsWith("### ") -> segments.add(MarkdownSegment.Header(trimmed.removePrefix("### "), 3))
            trimmed.startsWith("## ") -> segments.add(MarkdownSegment.Header(trimmed.removePrefix("## "), 2))
            trimmed.startsWith("# ") -> segments.add(MarkdownSegment.Header(trimmed.removePrefix("# "), 1))
            trimmed.startsWith("* ") -> segments.add(MarkdownSegment.BulletItem(trimmed.removePrefix("* ")))
            trimmed.startsWith("- ") -> segments.add(MarkdownSegment.BulletItem(trimmed.removePrefix("- ")))
            else -> segments.add(MarkdownSegment.Paragraph(line))
        }
    }

    if (inCodeBlock && codeBuilder.isNotEmpty()) {
        segments.add(
            MarkdownSegment.CodeBlock(
                code = codeBuilder.toString().trimEnd(),
                language = currentLanguage
            )
        )
    }

    return segments
}

fun parseInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentIndex = 0
        // Find **bold** pattern
        val boldRegex = Regex("""\*\*(.*?)\*\*""")
        val matches = boldRegex.findAll(text)

        for (match in matches) {
            // Append preceding regular text
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }
            // Append bold text
            val boldText = match.groupValues[1]
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                append(boldText)
            }
            currentIndex = match.range.last + 1
        }

        // Append remaining text
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
